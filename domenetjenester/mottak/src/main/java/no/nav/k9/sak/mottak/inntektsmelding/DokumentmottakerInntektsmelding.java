package no.nav.k9.sak.mottak.inntektsmelding;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
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
    private UttakTjeneste uttakTjeneste;
    private BehandlingRevurderingRepository revurderingRepository;
    private BehandlingRepository behandlingRepository;

    private BeanManager beanManager;

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentMottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           UttakTjeneste uttakTjeneste,
                                           ProsessTaskRepository prosessTaskRepository,
                                           BehandlingRepositoryProvider repositoryProvider,
                                           BeanManager beanManager) {
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;
        this.uttakTjeneste = uttakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.beanManager = beanManager;

        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument) { // #I1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        mottatteDokumentTjeneste.persisterInntektsmelding(behandling, mottattDokument);
        dokumentMottakerFelles.opprettTaskForÅStarteBehandlingFraInntektsmelding(mottattDokument, behandling);
    }

    void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { // #I6
            opprettTaskForÅVurdereInntektsmelding(fagsak, null, mottattDokument);
        } else {
            dokumentMottakerFelles.opprettRevurderingFraInntektsmelding(mottattDokument, fagsak, getBehandlingÅrsakType());
        }
    }

    void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument) { // #I2
        dokumentMottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), mottattDokument.getType());
        dokumentMottakerFelles.leggTilBehandlingsårsak(behandling, getBehandlingÅrsakType());
        dokumentMottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyInntektsmelding(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT);
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
    }

    void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling) {
        if (dokumentMottakerFelles.skalOppretteNyFørstegangsbehandling(avsluttetBehandling.getFagsak())) { // #I3
            opprettNyFørstegangFraAvslag(mottattDokument, fagsak, avsluttetBehandling);
        } else if (harAvslåttPeriode(avsluttetBehandling) && behandlingsoppretter.harBehandlingsresultatOpphørt(avsluttetBehandling)) { // #I4
            dokumentMottakerFelles.opprettRevurderingFraInntektsmelding(mottattDokument, fagsak, getBehandlingÅrsakType());
        } else { // #I5
            opprettTaskForÅVurdereInntektsmelding(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    protected BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    void opprettTaskForÅVurdereInntektsmelding(Fagsak fagsak, Behandling behandling, MottattDokument mottattDokument) {
        String behandlendeEnhetsId = dokumentMottakerFelles.hentBehandlendeEnhetTilVurderDokumentOppgave(fagsak, behandling);
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_DOKUMENT_TYPE, mottattDokument.getType().getKode());
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private Behandling opprettNyFørstegangFraAvslag(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling) {
        Behandling nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandling(mottattDokument, fagsak, avsluttetBehandling);
        behandlingsoppretter.opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(avsluttetBehandling, nyBehandling);
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(nyBehandling.getFagsakId(), nyBehandling.getId(), nyBehandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
        return nyBehandling;
    }

    protected boolean harAvslåttPeriode(Behandling avsluttetBehandling) {
        return uttakTjeneste.harAvslåttUttakPeriode(avsluttetBehandling.getUuid());
    }

    @Override
    public void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        doMottaDokument(mottattDokument, fagsak);
        doFireEvent(new Mottatt(fagsak.getYtelseType(), fagsak.getAktørId(), mottattDokument.getJournalpostId()));
    }
    
    /** Fyrer event via BeanManager slik at håndtering av events som subklasser andre events blir korrekt. */
    protected void doFireEvent(InntektsmeldingEvent event) {
        if (beanManager == null) {
            return;
        }
        beanManager.fireEvent(event, new Annotation[] {});
    }

    private void doMottaDokument(MottattDokument mottattDokument, Fagsak fagsak) {
        Optional<Behandling> sisteYtelsesbehandling = revurderingRepository.hentSisteYtelsesbehandling(fagsak.getId());

        if (sisteYtelsesbehandling.isEmpty()) {
            håndterIngenTidligereBehandling(fagsak, mottattDokument);
            return;
        }

        Behandling behandling = sisteYtelsesbehandling.get();
        boolean sisteYtelseErFerdigbehandlet = sisteYtelsesbehandling.map(Behandling::erSaksbehandlingAvsluttet).orElse(Boolean.FALSE);
        if (sisteYtelseErFerdigbehandlet) {
            Optional<Behandling> sisteAvsluttetBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
            behandling = sisteAvsluttetBehandling.orElse(behandling);
            // Håndter avsluttet behandling
            if (behandlingsoppretter.erAvslåttBehandling(behandling)
                || behandlingsoppretter.harBehandlingsresultatOpphørt(behandling)) {
                håndterAvslåttEllerOpphørtBehandling(mottattDokument, fagsak, behandling);
            } else {
                håndterAvsluttetTidligereBehandling(mottattDokument, fagsak);
            }
        } else {
            oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument);
        }
    }

    protected final boolean erAvslag(Behandling avsluttetBehandling) {
        return avsluttetBehandling.getBehandlingResultatType().isBehandlingsresultatAvslått();
    }

}
