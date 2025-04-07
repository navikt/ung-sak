package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

import java.util.Set;

@ApplicationScoped
@ProsessTask(value = OpprettEtterlysningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OpprettEtterlysningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "etterlysning.opprett";
    public static final String ETTERLYSNING_TYPE = "type";
    private EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste;

    public OpprettEtterlysningTask() {
        // CDI
    }

    @Inject
    public OpprettEtterlysningTask(EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste) {
        this.etterlysningProssesseringTjeneste = etterlysningProssesseringTjeneste;
    }

    @Override
    public Set<String> requiredProperties() {
        return Set.of(ETTERLYSNING_TYPE);
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var behandlingId = prosessTaskData.getBehandlingId();
        final var etterlysningType = EtterlysningType.fraKode(prosessTaskData.getPropertyValue(ETTERLYSNING_TYPE));
        etterlysningProssesseringTjeneste.opprett(Long.parseLong(behandlingId), etterlysningType);
    }
}
