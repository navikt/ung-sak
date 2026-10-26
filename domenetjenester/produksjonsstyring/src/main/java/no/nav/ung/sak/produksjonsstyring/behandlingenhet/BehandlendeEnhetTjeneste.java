package no.nav.ung.sak.produksjonsstyring.behandlingenhet;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.typer.AktørId;

import java.util.*;
import java.util.stream.Collectors;

@Dependent
public class BehandlendeEnhetTjeneste {

    private final EnhetsTjeneste enhetsTjeneste;
    private final BehandlingEnhetEventPubliserer eventPubliserer;
    private final BehandlingRepository behandlingRepository;
    private final HistorikkinnslagRepository historikkinnslagRepository;
    private final PersonopplysningRepository personopplysningRepository;
    private final BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    @Inject
    public BehandlendeEnhetTjeneste(EnhetsTjeneste enhetsTjeneste,
                                    BehandlingEnhetEventPubliserer eventPubliserer,
                                    BehandlingRepositoryProvider provider,
                                    BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.enhetsTjeneste = enhetsTjeneste;
        this.eventPubliserer = eventPubliserer;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.historikkinnslagRepository = provider.getHistorikkinnslagRepository();
        this.personopplysningRepository = provider.getPersonopplysningRepository();
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    // Alle aktuelle enheter
    public List<OrganisasjonsEnhet> hentEnhetListe(FagsakYtelseType ytelseType) {
        return enhetsTjeneste.hentEnhetListe(ytelseType);
    }

    public Map<BehandlingDel, OrganisasjonsEnhet> finnBehandlendeEnheterFor(Fagsak fagsak) {
        var behandlingstemaPerDel = behandlingstemaPerDel(fagsak.getYtelseType());
        Map<BehandlingDel, OrganisasjonsEnhet> enheter = new EnumMap<>(BehandlingDel.class);
        behandlingstemaPerDel.forEach((del, behandlingstema) ->
            enheter.put(del, finnBehandlendeEnhetFor(fagsak, del, behandlingstema)));
        return enheter;
    }

    public OrganisasjonsEnhet finnBehandlendeEnhetFor(Fagsak fagsak, BehandlingDel behandlingDel) {
        String behandlingstema = behandlingstemaPerDel(fagsak.getYtelseType()).get(behandlingDel);
        return finnBehandlendeEnhetFor(fagsak, behandlingDel, behandlingstema);
    }

    private OrganisasjonsEnhet finnBehandlendeEnhetFor(Fagsak fagsak, BehandlingDel behandlingDel, String behandlingstema) {
        FagsakYtelseType ytelseType = fagsak.getYtelseType();
        Optional<OrganisasjonsEnhet> forrigeEnhet = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .flatMap(b -> behandlingAnsvarligRepository.hentBehandlingAnsvarlig(b.getId(), behandlingDel))
            .filter(ba -> ba.getBehandlendeEnhet() != null && enhetsTjeneste.finnOrganisasjonsEnhet(ytelseType, behandlingstema, ba.getBehandlendeEnhet()).isPresent())
            .map(BehandlingAnsvarlig::getBehandlendeOrganisasjonsEnhet);
        return forrigeEnhet.orElseGet(() -> enhetsTjeneste.hentEnhetSjekkKunAktør(fagsak.getAktørId(), ytelseType, behandlingstema));
    }

    private static Map<BehandlingDel, String> behandlingstemaPerDel(FagsakYtelseType ytelseType) {
        return ytelseType == FagsakYtelseType.AKTIVITETSPENGER
            ? Map.of(BehandlingDel.SENTRAL, "YTELSE", BehandlingDel.NAV_KONTOR, "INNGANGSVILKÅR")
            : Collections.singletonMap(BehandlingDel.SENTRAL, null);
    }


    // Brukes for å sjekke om det er behov for å flytte eller endre til spesialenheter når saken tas av vent.
    // Returnerer kun deler der enheten skal endres.
    public Map<BehandlingDel, OrganisasjonsEnhet> sjekkEnheterEtterEndring(Behandling behandling) {
        var ytelseType = behandling.getFagsakYtelseType();
        var ansvarlige = behandlingAnsvarligRepository.hentBehandlingAnsvarlige(behandling.getId());
        var hovedPerson = behandling.getAktørId();
        var allePersoner = new HashSet<>(finnAktørIdFraPersonopplysninger(behandling));
        allePersoner.add(hovedPerson);

        Map<BehandlingDel, OrganisasjonsEnhet> endrede = new EnumMap<>(BehandlingDel.class);
        behandlingstemaPerDel(ytelseType).forEach((del, behandlingstema) -> {
            var ansvarlig = ansvarlige.get(del);
            if (ansvarlig == null) {
                return;
            }
            String enhetId = ansvarlig.getBehandlendeEnhet();
            enhetsTjeneste.finnEndretEnhetSjekkOppgittePersoner(ytelseType, behandlingstema, enhetId, hovedPerson, allePersoner)
                .filter(nyEnhet -> !nyEnhet.getEnhetId().equals(enhetId))
                .ifPresent(nyEnhet -> endrede.put(del, nyEnhet));
        });
        return endrede;
    }


    private Set<AktørId> finnAktørIdFraPersonopplysninger(Behandling behandling) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandling.getId())
            .flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon)
            .map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .map(PersonopplysningEntitet::getAktørId)
            .collect(Collectors.toSet());
    }

    // Oppdaterer behandlende enhet og sikre at dvh oppdateres (via event)
    public void oppdaterBehandlendeEnhet(Behandling behandling, BehandlingDel behandlingDel, OrganisasjonsEnhet nyEnhet, HistorikkAktør endretAv, String begrunnelse) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        if (endretAv != null) {
            BehandlingAnsvarlig behandlingAnsvarlig = behandlingAnsvarligRepository.hentBehandlingAnsvarlig(behandling.getId(), behandlingDel).orElse(null);
            lagHistorikkInnslagForByttBehandlendeEnhet(behandling, behandlingDel, behandlingAnsvarlig, nyEnhet, begrunnelse, endretAv);
        }

        behandlingAnsvarligRepository.setBehandlendeEnhet(behandling.getId(), behandlingDel, nyEnhet, begrunnelse);

        behandlingRepository.lagre(behandling, lås);
        eventPubliserer.fireEvent(behandling);
    }

    private void lagHistorikkInnslagForByttBehandlendeEnhet(Behandling behandling, BehandlingDel behandlingDel, BehandlingAnsvarlig behandlingAnsvarlig, OrganisasjonsEnhet nyEnhet, String begrunnelse, HistorikkAktør aktør) {
        OrganisasjonsEnhet eksisterende = behandlingAnsvarlig != null ? behandlingAnsvarlig.getBehandlendeOrganisasjonsEnhet() : null;
        String fraMessage = eksisterende != null ? eksisterende.getEnhetId() + " " + eksisterende.getEnhetNavn() : "ukjent";
        var innslagBuilder = new Historikkinnslag.Builder();
        innslagBuilder.medAktør(aktør);
        innslagBuilder.medTittel("Behandlende enhet er endret");
        String delTekst = behandlingDel == BehandlingDel.NAV_KONTOR ? "Behandlende Nav-kontor" : "Behandlende enhet";
        innslagBuilder.addLinje(delTekst + " er endret fra " + fraMessage + " til " + nyEnhet.getEnhetId() + " " + nyEnhet.getEnhetNavn());
        if (begrunnelse != null && !begrunnelse.isBlank()) {
            innslagBuilder.addLinje(begrunnelse);
        }
        innslagBuilder.medBehandlingId(behandling.getId());
        innslagBuilder.medFagsakId(behandling.getFagsakId());
        historikkinnslagRepository.lagre(innslagBuilder.build());
    }
}
