package no.nav.ung.sak.behandling.prosessering;

import no.nav.k9.prosesstask.api.BatchProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

/**
 * Baseklasse for batch-tasks som skal opprette én child-task dersom det ikke allerede finnes
 * en aktiv eller feilet task av samme type (duplikatbeskyttelse).
 * <p>
 * Mønsteret som abstraheres:
 * <ol>
 *   <li>Sjekk om det finnes eksisterende child-tasks med status FEILET/KLAR/VETO (med valgfritt filter)</li>
 *   <li>Hvis ja → avbryt for å unngå duplikater</li>
 *   <li>Hvis nei → opprett og lagre en ny child-task</li>
 * </ol>
 */
public abstract class DuplikatbeskyttetBatchTask implements BatchProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(DuplikatbeskyttetBatchTask.class);

    private static final List<ProsessTaskStatus> BLOKKERENDE_STATUSER = List.of(
        ProsessTaskStatus.FEILET,
        ProsessTaskStatus.KLAR,
        ProsessTaskStatus.VETO
    );

    private final ProsessTaskTjeneste prosessTaskTjeneste;

    protected DuplikatbeskyttetBatchTask() {
        // CDI proxy
        this.prosessTaskTjeneste = null;
    }

    protected DuplikatbeskyttetBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    /**
     * Tasknavn for child-tasken som skal opprettes.
     */
    protected abstract String childTaskName();

    /**
     * Opprett og konfigurer child-task-dataen som skal lagres.
     */
    protected abstract ProsessTaskData createChildTaskData();

    /**
     * Filter for å avgjøre hvilke eksisterende tasks som regnes som duplikater.
     * Default: tasks uten saksnummer (globale tasks).
     */
    protected Predicate<ProsessTaskData> duplikatFilter() {
        return it -> it.getSaksnummer() == null;
    }

    /**
     * Kalles før duplikatsjekk. Returner {@code false} for å avbryte tidlig (f.eks. feature toggle).
     */
    protected boolean isEnabled() {
        return true;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (!isEnabled()) {
            log.info("{} er deaktivert, oppretter ikke child-task", getClass().getSimpleName());
            return;
        }

        if (harEksisterendeChildTask()) {
            log.info("{} fant eksisterende child-task med status FEILET/KLAR/VETO for {}. Oppretter ikke duplikat.",
                getClass().getSimpleName(), childTaskName());
            return;
        }

        ProsessTaskData childTask = createChildTaskData();
        prosessTaskTjeneste.lagre(childTask);
    }

    private boolean harEksisterendeChildTask() {
        var filter = duplikatFilter();
        for (var status : BLOKKERENDE_STATUSER) {
            if (prosessTaskTjeneste.finnAlle(childTaskName(), status).stream().anyMatch(filter)) {
                return true;
            }
        }
        return false;
    }

    protected ProsessTaskTjeneste getProsessTaskTjeneste() {
        return prosessTaskTjeneste;
    }
}

