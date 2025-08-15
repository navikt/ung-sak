package no.nav.ung.sak.etterlysning.publisering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.ung.sak.etterlysning.EtterlysningProssesseringTjeneste;
import no.nav.ung.sak.metrikker.bigquery.BigQueryDataset;
import no.nav.ung.sak.metrikker.bigquery.BigQueryKlient;

import java.util.List;

@ApplicationScoped
@ProsessTask(value = PubliserEtterlysningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserEtterlysningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "etterlysning.publiser";
    public static final String ETTERLYSNING_ID = "etterlysningId";
    private EtterlysningRepository etterlysningRepository;
    private BehandlingRepository behandlingRepository;
    private BigQueryKlient bigQueryKlient;

    public PubliserEtterlysningTask() {
        // CDI
    }

    @Inject
    public PubliserEtterlysningTask(EtterlysningRepository etterlysningRepository, BehandlingRepository behandlingRepository, BigQueryKlient bigQueryKlient) {
        this.etterlysningRepository = etterlysningRepository;
        this.behandlingRepository = behandlingRepository;

        this.bigQueryKlient = bigQueryKlient;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var etterlysning = etterlysningRepository.hentEtterlysning(Long.parseLong(prosessTaskData.getPropertyValue(ETTERLYSNING_ID)));
        publiser(behandlingRepository.hentBehandling(etterlysning.getBehandlingId()), etterlysning);
    }

    private void publiser(Behandling behandling, Etterlysning etterlysning) {
        var record = new EtterlysningRecord(behandling.getFagsak().getSaksnummer(),
            etterlysning.getEksternReferanse(),
            etterlysning.getType(),
            etterlysning.getStatus(),
            etterlysning.getPeriode(),
            etterlysning.getFrist(),
            etterlysning.getEndretTidspunkt() != null ? etterlysning.getEndretTidspunkt() : etterlysning.getOpprettetTidspunkt());
        bigQueryKlient.publish(BigQueryDataset.UNG_SAK_STATISTIKK_DATASET, EtterlysningRecord.ETTERLYSNING_TABELL, List.of(record));
    }

}
