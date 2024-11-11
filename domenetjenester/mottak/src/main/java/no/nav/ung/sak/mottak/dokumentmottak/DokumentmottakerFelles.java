package no.nav.ung.sak.mottak.dokumentmottak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.typer.JournalpostId;

@Dependent
public class DokumentmottakerFelles {

    private final ProsessTaskTjeneste prosessTaskRepository;
    private final HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Inject
    public DokumentmottakerFelles(ProsessTaskTjeneste prosessTaskRepository,
                                  HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    public void opprettTaskForÅStarteBehandlingMedNySøknad(Behandling behandling, JournalpostId journalpostId) {
        ProsessTaskData prosessTaskData =  ProsessTaskData.forProsessTask(StartBehandlingTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);

        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, journalpostId, HistorikkinnslagType.BEH_STARTET);
    }

    public void opprettHistorikkinnslagForVedlegg(Long fagsakId, JournalpostId journalpostId, Brevkode dokumentTypeId) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(fagsakId, journalpostId, dokumentTypeId);
    }

    public ProsessTaskData opprettTaskForÅStarteBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData =  ProsessTaskData.forProsessTask(StartBehandlingTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        return prosessTaskData;
    }
}
