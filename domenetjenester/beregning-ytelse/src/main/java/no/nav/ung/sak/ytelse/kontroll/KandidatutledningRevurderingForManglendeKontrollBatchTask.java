package no.nav.ung.sak.ytelse.kontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingKandidaterRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Batchtask som utleder kandidater for revurdering grunnet manglende kontroll
 * <p>
 * Kjører hvert 15. minutt.
 */
@ApplicationScoped
@ProsessTask(value = KandidatutledningRevurderingForManglendeKontrollBatchTask.TASKNAME, cronExpression = "* */15 * * * *", maxFailedRuns = 1)
public class KandidatutledningRevurderingForManglendeKontrollBatchTask implements ProsessTaskHandler {

    public static final String TASKNAME = "batch.kandidatManglendeKontroll";
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private BehandlingKandidaterRepository behandlingKandidaterRepository;

    KandidatutledningRevurderingForManglendeKontrollBatchTask() {
    }

    @Inject
    public KandidatutledningRevurderingForManglendeKontrollBatchTask(ProsessTaskTjeneste prosessTaskTjeneste, BehandlingKandidaterRepository behandlingRepository) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.behandlingKandidaterRepository = behandlingRepository;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var kandidatBehandlinger = behandlingKandidaterRepository.hentAvsluttedeBehandlingerEtter(LocalDateTime.now().minusMinutes(15));
        final var behandlingerGruppertPåFagsak = kandidatBehandlinger.stream().collect(Collectors.groupingBy(Behandling::getFagsakId));
        final var sisteAvsluttetBehandlingPrFagsak = behandlingerGruppertPåFagsak
            .values()
            .stream()
            .map(KandidatutledningRevurderingForManglendeKontrollBatchTask::sisteAvsluttet)
            .collect(Collectors.toSet());
        opprettProsessTask(sisteAvsluttetBehandlingPrFagsak);
    }

    private static Behandling sisteAvsluttet(List<Behandling> behandlings) {
        return behandlings.stream().max(Comparator.comparing(Behandling::getAvsluttetDato)).orElseThrow();
    }

    private void opprettProsessTask(Set<Behandling> kandidater) {
        ProsessTaskGruppe taskGruppeTilRevurderinger = new ProsessTaskGruppe();
        var revurderTasker = kandidater
            .stream()
            .map(behandling -> {
                ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(VurderRevurderingForManglendeKontrollTask.class);
                tilVurderingTask.setBehandling(behandling.getFagsakId(), behandling.getId());
                return tilVurderingTask;
            }).toList();

        taskGruppeTilRevurderinger.addNesteParallell(revurderTasker);
        prosessTaskTjeneste.lagre(taskGruppeTilRevurderinger);
    }


}
