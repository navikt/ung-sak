package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

@ApplicationScoped
@ProsessTask(value = OpprettEtterlysningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OpprettEtterlysningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "etterlysning.opprett";
    public static final String ETTERLYSNING_TYPE = "type";

    private InntektkontrollEtterlysningHåndterer inntektkontrollEtterlysningOppretter;

    public OpprettEtterlysningTask() {
        // CDI
    }

    @Inject
    public OpprettEtterlysningTask(InntektkontrollEtterlysningHåndterer inntektkontrollEtterlysningOppretter) {
        this.inntektkontrollEtterlysningOppretter = inntektkontrollEtterlysningOppretter;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var behandlingId = prosessTaskData.getBehandlingId();
        final var etterlysningType = EtterlysningType.fraKode(prosessTaskData.getPropertyValue(ETTERLYSNING_TYPE));
        switch (etterlysningType) {
            case EtterlysningType.UTTALELSE_KONTROLL_INNTEKT:
                inntektkontrollEtterlysningOppretter.hånterEtterlysning(Long.parseLong(behandlingId));
                break;
            default:
                throw new IllegalArgumentException("Ukjent etterlysningstype: " + etterlysningType);
        }



    }
}
