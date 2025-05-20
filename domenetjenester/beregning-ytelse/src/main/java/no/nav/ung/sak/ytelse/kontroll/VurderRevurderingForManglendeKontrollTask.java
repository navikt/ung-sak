package no.nav.ung.sak.ytelse.kontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;


/**
 * Task som oppretter revurdering dersom manglende kontroll
 */
@ApplicationScoped
@ProsessTask(value = VurderRevurderingForManglendeKontrollTask.TASKNAME)
public class VurderRevurderingForManglendeKontrollTask implements ProsessTaskHandler {

    public static final String TASKNAME = "inntektskontroll.vurderRevurderingForManglendeKontroll";
    private ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    VurderRevurderingForManglendeKontrollTask() {
    }

    @Inject
    public VurderRevurderingForManglendeKontrollTask(ProsessTaskTjeneste prosessTaskTjeneste,
                                                     ManglendeKontrollperioderTjeneste manglendeKontrollperioderTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.manglendeKontrollperioderTjeneste = manglendeKontrollperioderTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var behandligId = Long.parseLong(prosessTaskData.getBehandlingId());
        final var fagsakId = prosessTaskData.getFagsakId();
        final var revurderingTask = manglendeKontrollperioderTjeneste.lagProsesstaskForRevurderingGrunnetManglendeKontrollAvInntekt(behandligId, fagsakId);
        revurderingTask.ifPresent(taskData -> prosessTaskTjeneste.lagre(taskData));
    }

}
