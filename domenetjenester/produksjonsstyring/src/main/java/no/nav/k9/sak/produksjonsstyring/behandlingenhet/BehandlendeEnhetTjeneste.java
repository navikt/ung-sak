package no.nav.k9.sak.produksjonsstyring.behandlingenhet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class BehandlendeEnhetTjeneste {

    private EnhetsTjeneste enhetsTjeneste;
    private BehandlingEnhetEventPubliserer eventPubliserer;
    private BehandlingRepository behandlingRepository;
    private HistorikkRepository historikkRepository;
    private PersonopplysningRepository personopplysningRepository;

    public BehandlendeEnhetTjeneste() {
        // For CDI
    }

    @Inject
    public BehandlendeEnhetTjeneste(EnhetsTjeneste enhetsTjeneste,
                                    BehandlingEnhetEventPubliserer eventPubliserer,
                                    BehandlingRepositoryProvider provider) {
        this.enhetsTjeneste = enhetsTjeneste;
        this.eventPubliserer = eventPubliserer;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.historikkRepository = provider.getHistorikkRepository();
        this.personopplysningRepository = provider.getPersonopplysningRepository();
    }

    // Alle aktuelle enheter
    public List<OrganisasjonsEnhet> hentEnhetListe(FagsakYtelseType ytelseType) {
        return enhetsTjeneste.hentEnhetListe(ytelseType);
    }

    // Brukes ved opprettelse av oppgaver før behandling har startet
    public OrganisasjonsEnhet finnBehandlendeEnhetFor(Fagsak fagsak) {
        Optional<OrganisasjonsEnhet> forrigeEnhet = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .filter(b -> gyldigEnhetNfpNk(fagsak.getYtelseType(), b.getBehandlendeEnhet()))
            .map(Behandling::getBehandlendeOrganisasjonsEnhet);
        return forrigeEnhet.orElse(enhetsTjeneste.hentEnhetSjekkKunAktør(fagsak.getAktørId(), fagsak.getYtelseType()));
    }

    // Sjekk om angitt journalførende enhet er gyldig for enkelte oppgaver
    public boolean gyldigEnhetNfpNk(FagsakYtelseType ytelseType, String enhetId) {
        return enhetsTjeneste.finnOrganisasjonsEnhet(ytelseType, enhetId).isPresent();
    }
    
    // Brukes for å sjekke om behandling skal flyttes etter endringer i NORG2-oppsett
    public Optional<OrganisasjonsEnhet> sjekkOppdatertEnhetEtterReallokering(Behandling behandling) {
        OrganisasjonsEnhet enhet = finnBehandlendeEnhetFor(behandling.getFagsak());
        if (enhet.getEnhetId().equals(behandling.getBehandlendeEnhet())) {
            return Optional.empty();
        }
        return Optional.of(getOrganisasjonsEnhetEtterEndring(behandling.getFagsak(), enhet, behandling.getAktørId(), new HashSet<>()).orElse(enhet));
    }
    
    
    // Brukes for å sjekke om det er behov for å flytte eller endre til spesialenheter når saken tas av vent.
    public Optional<OrganisasjonsEnhet> sjekkEnhetEtterEndring(Behandling behandling) {
        var enhet = behandling.getBehandlendeOrganisasjonsEnhet();
        if (enhet.equals(enhetsTjeneste.getEnhetKlage())) {
            return Optional.empty();
        }
        var oppdatertEnhet = getOrganisasjonsEnhetEtterEndring(behandling, enhet).orElse(enhet);
        return enhet.equals(oppdatertEnhet) ? Optional.empty() : Optional.of(oppdatertEnhet);
    }

    private Optional<OrganisasjonsEnhet> getOrganisasjonsEnhetEtterEndring(Behandling behandling, OrganisasjonsEnhet enhet) {
        AktørId hovedPerson = behandling.getAktørId();
        Set<AktørId> allePersoner = new HashSet<>();
        allePersoner.add(hovedPerson);
        Optional.ofNullable(behandling.getFagsak().getPleietrengendeAktørId()).ifPresent(allePersoner::add);
        Optional.ofNullable(behandling.getFagsak().getRelatertPersonAktørId()).ifPresent(allePersoner::add);

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
            lagHistorikkInnslagForByttBehandlendeEnhet(behandling, nyEnhet, begrunnelse, endretAv);
        }
        behandling.setBehandlendeEnhet(nyEnhet);
        behandling.setBehandlendeEnhetÅrsak(begrunnelse);

        behandlingRepository.lagre(behandling, lås);
        eventPubliserer.fireEvent(behandling);
    }

    private void lagHistorikkInnslagForByttBehandlendeEnhet(Behandling behandling, OrganisasjonsEnhet nyEnhet, String begrunnelse, HistorikkAktør aktør) {
        OrganisasjonsEnhet eksisterende = behandling.getBehandlendeOrganisasjonsEnhet();
        String fraMessage = eksisterende != null ? eksisterende.getEnhetId() + " " + eksisterende.getEnhetNavn() : "ukjent";
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BYTT_ENHET)
            .medEndretFelt(HistorikkEndretFeltType.BEHANDLENDE_ENHET,
                fraMessage,
                nyEnhet.getEnhetId() + " " + nyEnhet.getEnhetNavn())
            .medBegrunnelse(begrunnelse);

        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(aktør);
        innslag.setType(HistorikkinnslagType.BYTT_ENHET);
        innslag.setBehandlingId(behandling.getId());
        builder.build(innslag);
        historikkRepository.lagre(innslag);
    }
}
