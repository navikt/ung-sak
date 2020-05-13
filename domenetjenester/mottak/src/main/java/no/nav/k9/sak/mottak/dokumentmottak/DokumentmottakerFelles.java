package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDateTime;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@Dependent
public class DokumentmottakerFelles {

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private Behandlingsoppretter behandlingsoppretter;

    @SuppressWarnings("unused")
    private DokumentmottakerFelles() { // NOSONAR
        // For CDI
    }

    @Inject
    public DokumentmottakerFelles(BehandlingRepositoryProvider repositoryProvider,
                                  ProsessTaskRepository prosessTaskRepository,
                                  BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                  HistorikkinnslagTjeneste historikkinnslagTjeneste,
                                  MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                  Behandlingsoppretter behandlingsoppretter) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
    }

    public void leggTilBehandlingsårsak(Behandling behandling, BehandlingÅrsakType behandlingÅrsak) {
        BehandlingÅrsak.Builder builder = BehandlingÅrsak.builder(behandlingÅrsak);
        behandling.getOriginalBehandling().ifPresent(builder::medOriginalBehandling);
        builder.buildFor(behandling);

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
    }

    public void opprettTaskForÅStarteBehandlingMedNySøknad(Behandling behandling, JournalpostId journalpostId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
        
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, journalpostId, HistorikkinnslagType.BEH_STARTET);
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling, HistorikkinnslagType historikkinnslagType, LocalDateTime frist, Venteårsak venteårsak) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling, historikkinnslagType, frist, venteårsak);
    }

    public void opprettHistorikkinnslagForVedlegg(Long fagsakId, JournalpostId journalpostId, Brevkode dokumentTypeId) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(fagsakId, journalpostId, dokumentTypeId);
    }

    public String hentBehandlendeEnhetTilVurderDokumentOppgave(Fagsak sak, Behandling behandling) {
        if (behandling == null) {
            return finnEnhetFraFagsak(sak);
        }
        return behandling.getBehandlendeEnhet();
    }

    private String finnEnhetFraFagsak(Fagsak sak) {
        OrganisasjonsEnhet organisasjonsEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(sak);
        return organisasjonsEnhet.getEnhetId();
    }

    public void opprettHistorikkinnslagForBehandlingOppdatertMedNyInntektsmelding(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(behandling, behandlingÅrsakType);
    }

    public Behandling opprettRevurderingFraInntektsmelding(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling revurdering = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        mottatteDokumentTjeneste.persisterDokumentinnhold(revurdering, mottattDokument);
        opprettTaskForÅStarteBehandlingFraInntektsmelding(mottattDokument, revurdering);
        return revurdering;
    }

    public void opprettTaskForÅStarteBehandlingFraInntektsmelding(MottattDokument mottattDokument, Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
        
        opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), mottattDokument.getType());
    }
    
    public boolean skalOppretteNyFørstegangsbehandling(Fagsak fagsak) {
        if (mottatteDokumentTjeneste.erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(fagsak)) {
            return !mottatteDokumentTjeneste.harFristForInnsendingAvDokGåttUt(fagsak);
        }
        return false;
    }

}
