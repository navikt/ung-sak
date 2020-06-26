package no.nav.k9.sak.mottak.inntektsmelding;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.iay.inntektsmelding.InntektsmeldingEvent;
import no.nav.k9.sak.domene.iay.inntektsmelding.InntektsmeldingEvent.Mottatt;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.mottak.dokumentmottak.Kompletthetskontroller;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(Brevkode.INNTEKTSMELDING)
public class DokumentmottakerInntektsmelding implements Dokumentmottaker {

    private Behandlingsoppretter behandlingsoppretter;
    private ProsessTaskRepository prosessTaskRepository;
    private Kompletthetskontroller kompletthetskontroller;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private DokumentmottakerFelles dokumentMottakerFelles;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingRepository behandlingRepository;

    private BeanManager beanManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    private InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentMottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           ProsessTaskRepository prosessTaskRepository,
                                           BehandlingRepositoryProvider repositoryProvider,
                                           BeanManager beanManager) {
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;
        this.prosessTaskRepository = prosessTaskRepository;
        this.beanManager = beanManager;

        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
    }

    void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument) { // #I1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        mottatteDokumentTjeneste.persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(behandling, mottattDokument);
        dokumentMottakerFelles.opprettTaskForÅStarteBehandlingFraInntektsmelding(mottattDokument, behandling);
    }

    void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Behandling behandling) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(behandling.getFagsak())) { // #I6
            opprettTaskForÅVurdereInntektsmelding(behandling.getFagsak(), null, mottattDokument);
        } else {
            dokumentMottakerFelles.opprettRevurderingFraInntektsmelding(mottattDokument, behandling, getBehandlingÅrsakType());
        }
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

    void opprettTaskForÅVurdereInntektsmelding(Fagsak fagsak, Behandling behandling, MottattDokument mottattDokument) {
        String behandlendeEnhetsId = dokumentMottakerFelles.hentBehandlendeEnhetTilVurderDokumentOppgave(fagsak, behandling);
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_DOKUMENT_TYPE, mottattDokument.getType().getKode());
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId()); // tar ikke med behandling her siden det evt. gjelder ny
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
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

    /**
     * Fyrer event via BeanManager slik at håndtering av events som subklasser andre events blir korrekt.
     */
    protected void doFireEvent(InntektsmeldingEvent event) {
        if (beanManager == null) {
            return;
        }
        beanManager.fireEvent(event, new Annotation[]{});
    }

    private void doMottaDokument(MottattDokument mottattDokument, Fagsak fagsak) {
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());

        if (sisteYtelsesbehandling.isEmpty()) {
            håndterIngenTidligereBehandling(fagsak, mottattDokument);
            return;
        }

        Behandling behandling = sisteYtelsesbehandling.get();
        sjekkBehandlingKanLåses(behandling); // sjekker at kan låses (dvs ingen andre prosesserer den samtidig, hvis ikke kommer vi tilbake senere en gang)

        boolean sisteYtelseErFerdigbehandlet = sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.FALSE);
        if (sisteYtelseErFerdigbehandlet) {
            Optional<Behandling> sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
            behandling = sisteAvsluttetBehandling.orElse(behandling);
            // Håndter avsluttet behandling
            håndterAvsluttetTidligereBehandling(mottattDokument, behandling);
        } else {
            oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument);
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
