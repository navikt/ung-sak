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

    // TODO TSFF-3237: offisielle behandlingstema-koder for aktivitetspenger er ikke avklart. null gir oppslag uten behandlingstema.
    private static final String AKTIVITETSPENGER_BEHANDLINGSTEMA_YTELSE = null;
    private static final String AKTIVITETSPENGER_BEHANDLINGSTEMA_INNGANGSVILKÅR = null;

    private EnhetsTjeneste enhetsTjeneste;
    private BehandlingEnhetEventPubliserer eventPubliserer;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

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

    // Kun SENTRAL. Brukes der behandlingen ikke har NAV_KONTOR-del, f.eks. klage.
    public OrganisasjonsEnhet finnBehandlendeEnhetFor(Fagsak fagsak) {
        return finnBehandlendeEnhetFor(fagsak, BehandlingDel.SENTRAL);
    }

    public Map<BehandlingDel, OrganisasjonsEnhet> finnBehandlendeEnheterFor(Fagsak fagsak) {
        if (fagsak.getYtelseType() == FagsakYtelseType.AKTIVITETSPENGER) {
            return Map.of(
                BehandlingDel.SENTRAL, finnBehandlendeEnhetFor(fagsak, BehandlingDel.SENTRAL),
                BehandlingDel.NAV_KONTOR, finnBehandlendeEnhetFor(fagsak, BehandlingDel.NAV_KONTOR));
        }
        return Map.of(BehandlingDel.SENTRAL, finnBehandlendeEnhetFor(fagsak, BehandlingDel.SENTRAL));
    }

    private OrganisasjonsEnhet finnBehandlendeEnhetFor(Fagsak fagsak, BehandlingDel behandlingDel) {
        FagsakYtelseType ytelseType = fagsak.getYtelseType();
        String behandlingstema = behandlingstema(ytelseType, behandlingDel);
        Optional<OrganisasjonsEnhet> forrigeEnhet = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .flatMap(b -> behandlingAnsvarligRepository.hentBehandlingAnsvarlig(b.getId(), behandlingDel))
            .filter(ba -> ba.getBehandlendeEnhet() != null && enhetsTjeneste.finnOrganisasjonsEnhet(ytelseType, behandlingstema, ba.getBehandlendeEnhet()).isPresent())
            .map(BehandlingAnsvarlig::getBehandlendeOrganisasjonsEnhet);
        return forrigeEnhet.orElseGet(() -> enhetsTjeneste.hentEnhetSjekkKunAktør(fagsak.getAktørId(), ytelseType, behandlingstema));
    }

    private static String behandlingstema(FagsakYtelseType ytelseType, BehandlingDel behandlingDel) {
        if (ytelseType != FagsakYtelseType.AKTIVITETSPENGER) {
            return null;
        }
        return behandlingDel == BehandlingDel.SENTRAL
            ? AKTIVITETSPENGER_BEHANDLINGSTEMA_YTELSE
            : AKTIVITETSPENGER_BEHANDLINGSTEMA_INNGANGSVILKÅR;
    }

    // Sjekk om angitt journalførende enhet er gyldig for enkelte oppgaver
    public boolean gyldigEnhetNfpNk(FagsakYtelseType ytelseType, String enhetId) {
        return enhetsTjeneste.finnOrganisasjonsEnhet(ytelseType, enhetId).isPresent();
    }

    // Brukes for å sjekke om behandling skal flyttes etter endringer i NORG2-oppsett
    public Optional<OrganisasjonsEnhet> sjekkOppdatertEnhetEtterReallokering(Behandling behandling) {
        String behandlingensBehandlendeEnhet = behandlingAnsvarligRepository.hentBehandlingAnsvarlig(behandling.getId()).map(BehandlingAnsvarlig::getBehandlendeEnhet).orElse(null);
        OrganisasjonsEnhet enhet = finnBehandlendeEnhetFor(behandling.getFagsak());
        if (enhet.getEnhetId().equals(behandlingensBehandlendeEnhet)) {
            return Optional.empty();
        }
        return Optional.of(getOrganisasjonsEnhetEtterEndring(behandling.getFagsak(), enhet, behandling.getAktørId(), new HashSet<>()).orElse(enhet));
    }


    // Brukes for å sjekke om det er behov for å flytte eller endre til spesialenheter når saken tas av vent.
    public Optional<OrganisasjonsEnhet> sjekkEnhetEtterEndring(Behandling behandling) {
        OrganisasjonsEnhet enhet = behandlingAnsvarligRepository.hentBehandlingAnsvarlig(behandling.getId()).map(BehandlingAnsvarlig::getBehandlendeOrganisasjonsEnhet).orElse(null);
        if (Objects.equals(enhet, enhetsTjeneste.getEnhetKlage())) {
            return Optional.empty();
        }
        var oppdatertEnhet = getOrganisasjonsEnhetEtterEndring(behandling, enhet).orElse(enhet);
        return enhet.equals(oppdatertEnhet) ? Optional.empty() : Optional.of(oppdatertEnhet);
    }

    private Optional<OrganisasjonsEnhet> getOrganisasjonsEnhetEtterEndring(Behandling behandling, OrganisasjonsEnhet enhet) {
        AktørId hovedPerson = behandling.getAktørId();
        Set<AktørId> allePersoner = new HashSet<>();
        allePersoner.add(hovedPerson);
        allePersoner.addAll(finnAktørIdFraPersonopplysninger(behandling));

        return getOrganisasjonsEnhetEtterEndring(behandling.getFagsak(), enhet, hovedPerson, allePersoner);
    }


    private Optional<OrganisasjonsEnhet> getOrganisasjonsEnhetEtterEndring(Fagsak fagsak, OrganisasjonsEnhet enhet, AktørId hovedAktør, Set<AktørId> allePersoner) {
        allePersoner.add(hovedAktør);
        return enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(fagsak.getYtelseType(), enhet.getEnhetId(), hovedAktør, allePersoner);
    }

    private Set<AktørId> finnAktørIdFraPersonopplysninger(Behandling behandling) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandling.getId())
            .flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon)
            .map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .map(PersonopplysningEntitet::getAktørId)
            .collect(Collectors.toSet());
    }

    // Oppdaterer behandlende enhet og sikre at dvh oppdateres (via event)
    public void oppdaterBehandlendeEnhet(Behandling behandling, OrganisasjonsEnhet nyEnhet, HistorikkAktør endretAv, String begrunnelse) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        if (endretAv != null) {
            BehandlingAnsvarlig behandlingAnsvarlig = behandlingAnsvarligRepository.hentBehandlingAnsvarlig(behandling.getId()).orElse(null);
            lagHistorikkInnslagForByttBehandlendeEnhet(behandling, behandlingAnsvarlig, nyEnhet, begrunnelse, endretAv);
        }

        behandlingAnsvarligRepository.setBehandlendeEnhet(behandling.getId(), nyEnhet, begrunnelse);

        behandlingRepository.lagre(behandling, lås);
        eventPubliserer.fireEvent(behandling);
    }

    private void lagHistorikkInnslagForByttBehandlendeEnhet(Behandling behandling, BehandlingAnsvarlig behandlingAnsvarlig, OrganisasjonsEnhet nyEnhet, String begrunnelse, HistorikkAktør aktør) {
        OrganisasjonsEnhet eksisterende = behandlingAnsvarlig != null ? behandlingAnsvarlig.getBehandlendeOrganisasjonsEnhet() : null;
        String fraMessage = eksisterende != null ? eksisterende.getEnhetId() + " " + eksisterende.getEnhetNavn() : "ukjent";
        var innslagBuilder = new Historikkinnslag.Builder();
        innslagBuilder.medAktør(aktør);
        innslagBuilder.medTittel("Behandlende enhet er endret");
        innslagBuilder.addLinje("Behandlende enhet er endret fra " + fraMessage + " til " + nyEnhet.getEnhetId() + " " + nyEnhet.getEnhetNavn());
        if (begrunnelse != null && !begrunnelse.isBlank()) {
            innslagBuilder.addLinje(begrunnelse);
        }
        innslagBuilder.medBehandlingId(behandling.getId());
        innslagBuilder.medFagsakId(behandling.getFagsakId());
        historikkinnslagRepository.lagre(innslagBuilder.build());
    }
}
