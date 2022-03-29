package no.nav.k9.sak.behandling.revurdering.etterkontroll.batch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.tjeneste.AutomatiskEtterkontrollTjeneste;

/**
 * Henter ut behandlinger som har fått innvilget engangsstønad på bakgrunn av terminbekreftelsen,
 * for å etterkontrollere om rett antall barn har blitt født.
 * <p>
 * Vedtak er innvilget og fattet med bakgrunn i bekreftet terminbekreftelse
 * Det har gått minst 60 dager siden termin
 * Det er ikke registrert fødselsdato på barnet/barna
 * Det ikke allerede er opprettet revurderingsbehandling med en av disse årsakene:
 * Manglende fødsel i TPS
 * Manglende fødsel i TPS mellom uke 26 og 29
 * Avvik i antall barn
 * <p>
 * Ved avvik så opprettes det, hvis det ikke allerede finnes, revurderingsbehandling på saken
 */

@ApplicationScoped
@ProsessTask(value = AutomatiskEtterkontrollBatchTask.TASKTYPE, cronExpression = "0 15 7 * * *", maxFailedRuns = 1)
public class AutomatiskEtterkontrollBatchTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.etterkontroll";
    private AutomatiskEtterkontrollTjeneste tjeneste;

    @Inject
    public AutomatiskEtterkontrollBatchTask(AutomatiskEtterkontrollTjeneste tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        tjeneste.etterkontrollerBehandlinger();
    }
}
