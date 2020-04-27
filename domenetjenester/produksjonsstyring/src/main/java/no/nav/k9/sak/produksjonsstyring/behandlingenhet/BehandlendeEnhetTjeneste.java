package no.nav.k9.sak.produksjonsstyring.behandlingenhet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    }

    // Alle aktuelle enheter
    public List<OrganisasjonsEnhet> hentEnhetListe(FagsakYtelseType ytelseType) {
        return enhetsTjeneste.hentEnhetListe(ytelseType);
    }

    // Brukes ved opprettelse av oppgaver før behandling har startet
    public OrganisasjonsEnhet finnBehandlendeEnhetFraSøker(Fagsak fagsak) {
        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkRegistrerteRelasjoner(fagsak.getAktørId(), fagsak.getYtelseType());
        return enhet;
    }

    // Brukes ved opprettelse av førstegangsbehandling
    public OrganisasjonsEnhet finnBehandlendeEnhetFraSøker(Behandling behandling) {
        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkRegistrerteRelasjoner(behandling.getAktørId(), behandling.getFagsakYtelseType());
        return enhet;
    }

    // Brukes for å sjekke om det er behov for å endre til spesialenheter når saken tas av vent.
    public Optional<OrganisasjonsEnhet> sjekkEnhetVedGjenopptak(Behandling behandling) {
        return sjekkEnhetForBehandlingMedEvtKobletSak(behandling);
    }

    // Brukes for å utlede enhet ved opprettelse av klage, revurdering, ny førstegangsbehandling, innsyn mv. Vil normalt videreføre enhet fra tidligere behandling
    public Optional<OrganisasjonsEnhet> sjekkEnhetVedNyAvledetBehandling(Fagsak fagsak) {
        Optional<Behandling> opprinneligBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (!opprinneligBehandling.isPresent()) {
            return Optional.of(finnBehandlendeEnhetFraSøker(fagsak));
        }
        OrganisasjonsEnhet enhet = sjekkEnhetForBehandlingMedEvtKobletSak(opprinneligBehandling.get())
            .orElse(opprinneligBehandling.get().getBehandlendeOrganisasjonsEnhet());
        return Optional.of(enhet);
    }

    // Brukes for å utlede enhet ved opprettelse av klage, revurdering, ny førstegangsbehandling, innsyn mv. Vil normalt videreføre enhet fra tidligere behandling
    public Optional<OrganisasjonsEnhet> sjekkEnhetVedNyAvledetBehandling(Behandling opprinneligBehandling) {
        return sjekkEnhetForBehandlingMedEvtKobletSak(opprinneligBehandling);
    }

    private Optional<OrganisasjonsEnhet> sjekkEnhetForBehandlingMedEvtKobletSak(Behandling behandling) {
        AktørId hovedPerson = behandling.getAktørId();
        Optional<AktørId> kobletPerson = Optional.empty();
        List<AktørId> relatertePersoner = new ArrayList<>();

        return enhetsTjeneste.oppdaterEnhetSjekkRegistrerteRelasjoner(behandling.getFagsakYtelseType(), behandling.getBehandlendeOrganisasjonsEnhet().getEnhetId(),
            hovedPerson, kobletPerson, relatertePersoner);
    }

    // Sjekk om angitt journalførende enhet er gyldig for enkelte oppgaver
    public boolean gyldigEnhetNfpNk(FagsakYtelseType ytelseType, String enhetId) {
        return enhetsTjeneste.finnOrganisasjonsEnhet(ytelseType, enhetId).isPresent();
    }

    // Brukes for å sjekke om behandling skal flyttes etter endringer i NORG2-oppsett
    public Optional<OrganisasjonsEnhet> sjekkOppdatertEnhetEtterReallokering(Behandling behandling) {
        OrganisasjonsEnhet enhet = finnBehandlendeEnhetFraSøker(behandling);
        if (enhet.getEnhetId().equals(behandling.getBehandlendeEnhet())) {
            return Optional.empty();
        }
        return Optional.of(enhet);
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
