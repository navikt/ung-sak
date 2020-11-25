package no.nav.k9.sak.mottak;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@Dependent
public class SøknadmottakFelles {

    private ProsessTaskRepository prosessTaskRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @SuppressWarnings("unused")
    private SøknadmottakFelles() { // NOSONAR
        // For CDI
    }

    @Inject
    public SøknadmottakFelles(ProsessTaskRepository prosessTaskRepository, HistorikkinnslagTjeneste historikkinnslagTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
    }

    public void opprettTaskForÅStarteBehandlingMedNySøknad(Behandling behandling, JournalpostId journalpostId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);

        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, journalpostId, HistorikkinnslagType.BEH_STARTET);
    }
}
