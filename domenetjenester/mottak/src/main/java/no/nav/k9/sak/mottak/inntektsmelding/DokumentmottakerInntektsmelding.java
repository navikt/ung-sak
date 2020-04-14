package no.nav.k9.sak.mottak.inntektsmelding;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.mottak.dokumentmottak.Kompletthetskontroller;
import no.nav.k9.sak.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("INNTEKTSMELDING")
public class DokumentmottakerInntektsmelding extends DokumentmottakerYtelsesesrelatertDokument {

    private static final DokumentTypeId INNTEKTSMELDING = DokumentTypeId.INNTEKTSMELDING;
    private Behandlingsoppretter behandlingsoppretter;
    private ProsessTaskRepository prosessTaskRepository;
    private Kompletthetskontroller kompletthetskontroller;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private DokumentmottakerFelles dokumentMottakerFelles;
    private UttakTjeneste uttakTjeneste;

    @Inject
    public DokumentmottakerInntektsmelding(DokumentmottakerFelles dokumentMottakerFelles,
                                           MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                           Behandlingsoppretter behandlingsoppretter,
                                           Kompletthetskontroller kompletthetskontroller,
                                           UttakTjeneste uttakTjeneste,
                                           ProsessTaskRepository prosessTaskRepository,
                                           BehandlingRepositoryProvider repositoryProvider) {
        super(behandlingsoppretter,
            repositoryProvider);
        this.dokumentMottakerFelles = dokumentMottakerFelles;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.kompletthetskontroller = kompletthetskontroller;
        this.uttakTjeneste = uttakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void håndterIngenTidligereBehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { // #I1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, mottattDokument);
        dokumentMottakerFelles.opprettTaskForÅStarteBehandlingFraInntektsmelding(mottattDokument, behandling);
    }

    @Override
    public void håndterAvsluttetTidligereBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        if (behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)) { // #I6
            opprettTaskForÅVurdereInntektsmelding(fagsak, null, mottattDokument);
        } else {
            dokumentMottakerFelles.opprettRevurderingFraInntektsmelding(mottattDokument, fagsak, getBehandlingÅrsakType());
        }
    }

    @Override
    public void oppdaterÅpenBehandlingMedDokument(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { // #I2
        dokumentMottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), INNTEKTSMELDING);
        dokumentMottakerFelles.leggTilBehandlingsårsak(behandling, getBehandlingÅrsakType());
        dokumentMottakerFelles.opprettHistorikkinnslagForBehandlingOppdatertMedNyInntektsmelding(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT);
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
    }

    @Override
    public void håndterAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        if (dokumentMottakerFelles.skalOppretteNyFørstegangsbehandling(avsluttetBehandling.getFagsak())) { // #I3
            opprettNyFørstegangFraAvslag(mottattDokument, fagsak, avsluttetBehandling, INNTEKTSMELDING);
        } else if (harAvslåttPeriode(avsluttetBehandling) && behandlingsoppretter.harBehandlingsresultatOpphørt(avsluttetBehandling)) { // #I4
            dokumentMottakerFelles.opprettRevurderingFraInntektsmelding(mottattDokument, fagsak, getBehandlingÅrsakType());
        } else { // #I5
            opprettTaskForÅVurdereInntektsmelding(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    @Override
    protected BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
    }

    void opprettTaskForÅVurdereInntektsmelding(Fagsak fagsak, Behandling behandling, MottattDokument mottattDokument) {
        String behandlendeEnhetsId = dokumentMottakerFelles.hentBehandlendeEnhetTilVurderDokumentOppgave(mottattDokument, fagsak, behandling);
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_DOKUMENT_TYPE, DokumentTypeId.INNTEKTSMELDING.getKode());
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    Behandling opprettNyFørstegangFraAvslag(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, DokumentTypeId dokumentTypeId) {
        Behandling nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandling(mottattDokument, fagsak, avsluttetBehandling, dokumentTypeId);
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

}
