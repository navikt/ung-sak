package no.nav.k9.sak.behandling.revurdering.satsregulering;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.util.Tuple;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.k9.sak.typer.AktørId;

@Deprecated(since = "Kalkulus skal eie satsregulering")
@ApplicationScoped
@ProsessTask(AutomatiskArenaReguleringBatchTask.TASKTYPE)
public class AutomatiskArenaReguleringBatchTask implements ProsessTaskHandler {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final String KEY_SATS_DATO = "satsDato";
    public static final String KEY_DRY_RUN = "dryRun";
    public static final String TASKTYPE = "batch.arenaRegulering";
    static final LocalDate DATO = LocalDate.of(LocalDate.now().getYear(), 5, 1);
    private static final Logger log = LoggerFactory.getLogger(AutomatiskArenaReguleringBatchTask.class);
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public AutomatiskArenaReguleringBatchTask(BehandlingRepositoryProvider repositoryProvider,
                                              ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
    }

    @Override
    public void doTask(ProsessTaskData taskData) {
        final var satsDatoProperty = taskData.getPropertyValue(KEY_SATS_DATO);
        final var dryRunProperty = taskData.getPropertyValue(KEY_DRY_RUN);
        final boolean dryRun = Boolean.parseBoolean(dryRunProperty);
        final LocalDate satsDato = LocalDate.parse(satsDatoProperty, DATE_FORMATTER);

        List<Tuple<Long, AktørId>> tilVurdering = behandlingRevurderingRepository.finnSakerMedBehovForArenaRegulering(DATO, satsDato);
        if (dryRun) {
            tilVurdering.forEach(sak -> log.info("[DRYRUN] Vil revurdere sak {} for aktør {}", sak.getElement1(), sak.getElement2()));
        } else {
            tilVurdering.forEach(sak -> opprettReguleringTask(sak.getElement1(), sak.getElement2()));
        }
        log.info("Fant {} saker til revurdering", tilVurdering.size());
    }

    private void opprettReguleringTask(Long fagsakId, AktørId aktørId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskGrunnbelopReguleringTask.TASKTYPE);
        prosessTaskData.setFagsak(fagsakId, aktørId.getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
