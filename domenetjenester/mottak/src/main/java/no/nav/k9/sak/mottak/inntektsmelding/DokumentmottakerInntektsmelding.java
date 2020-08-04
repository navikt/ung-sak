package no.nav.k9.sak.mottak.inntektsmelding;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.iay.inntektsmelding.InntektsmeldingEvent;
import no.nav.k9.sak.domene.iay.inntektsmelding.InntektsmeldingEvent.Mottatt;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.mottak.dokumentmottak.Kompletthetskontroller;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(Brevkode.INNTEKTSMELDING)
public class DokumentmottakerInntektsmelding implements Dokumentmottaker {

    private Behandlingsoppretter behandlingsoppretter;
    private Kompletthetskontroller kompletthetskontroller;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private DokumentmottakerFelles dokumentMottakerFelles;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingRepository behandlingRepository;

    private BeanManager beanManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    private InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    DokumentmottakerInntektsmelding() {
        // for CDI
    }

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentMottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           BehandlingRepositoryProvider repositoryProvider,
                                           BeanManager beanManager) {
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;
        this.beanManager = beanManager;

        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
    }

    @Override
    public void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        doMottaDokument(mottattDokument, fagsak);
        doFireEvent(new Mottatt(fagsak.getYtelseType(), fagsak.getAktørId(), mottattDokument.getJournalpostId()));
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument, FagsakYtelseType ytelseType) {
        inntektsmeldingParser.parseInntektsmeldinger(mottattDokument);
    }

    void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument) { // #I2
        dokumentMottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), mottattDokument.getType());
        dokumentMottakerFelles.leggTilBehandlingsårsak(behandling, getBehandlingÅrsakType());
        dokumentMottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyInntektsmelding(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT);
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
    }

    protected BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    /**
     * Fyrer event via BeanManager slik at håndtering av events som subklasser andre events blir korrekt.
     */
    protected void doFireEvent(InntektsmeldingEvent event) {
        if (beanManager == null) {
            return;
        }
        beanManager.fireEvent(event, new Annotation[] {});
    }

    private void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument) { // #I1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        mottatteDokumentTjeneste.persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(behandling, mottattDokument);
        dokumentMottakerFelles.opprettTaskForÅStarteBehandlingFraInntektsmelding(mottattDokument, behandling);
    }

    private void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Behandling tidligereAvsluttetBehandling) {
        var sisteHenlagteFørstegangsbehandling = behandlingsoppretter.sisteHenlagteFørstegangsbehandling(tidligereAvsluttetBehandling.getFagsak());
        if (sisteHenlagteFørstegangsbehandling.isPresent()) { // #I6
            var nyFørstegangsbehandling = behandlingsoppretter.opprettNyFørstegangsbehandling(mottattDokument, sisteHenlagteFørstegangsbehandling.get().getFagsak(),
                sisteHenlagteFørstegangsbehandling.get());
            dokumentMottakerFelles.opprettTaskForÅStarteBehandlingFraInntektsmelding(mottattDokument, nyFørstegangsbehandling);
        } else {
            dokumentMottakerFelles.opprettRevurderingFraInntektsmelding(mottattDokument, tidligereAvsluttetBehandling, getBehandlingÅrsakType());
        }
    }

    private void doMottaDokument(MottattDokument mottattDokument, Fagsak fagsak) {
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());

        if (sisteYtelsesbehandling.isEmpty()) {
            håndterIngenTidligereBehandling(fagsak, mottattDokument);
            return;
        }

        Behandling sisteBehandling = sisteYtelsesbehandling.get();
        sjekkBehandlingKanLåses(sisteBehandling); // sjekker at kan låses (dvs ingen andre prosesserer den samtidig, hvis ikke kommer vi tilbake senere en gang)

        boolean sisteYtelseErFerdigbehandlet = sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.FALSE);
        if (sisteYtelseErFerdigbehandlet) {
            Optional<Behandling> sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
            sisteBehandling = sisteAvsluttetBehandling.orElse(sisteBehandling);
            // Håndter avsluttet behandling
            håndterAvsluttetTidligereBehandling(mottattDokument, sisteBehandling);
        } else {
            oppdaterÅpenBehandlingMedDokument(sisteBehandling, mottattDokument);
        }
    }

    private void sjekkBehandlingKanLåses(Behandling behandling) {
        var lås = behandlingLåsRepository.taLåsHvisLedig(behandling.getId());
        if (lås == null) {
            // noen andre holder på siden vi ikke fikk fatt på lås, så avbryter denne gang
            throw MottattInntektsmeldingFeil.FACTORY.behandlingPågårAvventerKnytteMottattDokumentTilBehandling(behandling.getId()).toException();
        }
    }
}
