package no.nav.ung.sak.behandling.prosessering;

import no.nav.k9.prosesstask.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
     * Tasktype for child-tasken som skal opprettes.
     */
    protected abstract TaskType getTaskType();

    /**
     * Sjekk for å avgjøre hvilke eksisterende tasks som regnes som duplikater.
     * Default: tasks uten saksnummer (globale tasks).
     */
    protected boolean erDuplikat(ProsessTaskData data) {
        return data.getSaksnummer() == null;
    }

    /**
     * Kalles før duplikatsjekk. Returner {@code false} for å avbryte tidlig (f.eks. feature toggle).
     */
    protected boolean isEnabled() {
        return true;
    }

    /**
     * Kalles etter at child-tasken er opprettet, men før den lagres. Overstyr for å sette
     * properties på child-tasken (f.eks. periode) som eventuelt {@link #erDuplikat(ProsessTaskData)}
     * er avhengig av.
     */
    protected void leggTilProperties(ProsessTaskData childTask) {
        // Standard: ingen ekstra properties
    }

    @Override
    public final void doTask(ProsessTaskData prosessTaskData) {
        if (!isEnabled()) {
            log.info("{} er deaktivert, oppretter ikke child-task", getClass().getSimpleName());
            return;
        }

        if (harEksisterendeChildTask()) {
            log.info("{} fant eksisterende child-task med status FEILET/KLAR/VETO for {}. Oppretter ikke duplikat.",
                getClass().getSimpleName(), getTaskType().value());
            return;
        }

        ProsessTaskData childTask = ProsessTaskData.forTaskType(getTaskType());
        leggTilProperties(childTask);
        prosessTaskTjeneste.lagre(childTask);
    }

    private boolean harEksisterendeChildTask() {
        for (var status : BLOKKERENDE_STATUSER) {
            if (prosessTaskTjeneste.finnAlle(getTaskType().value(), status).stream().anyMatch(this::erDuplikat)) {
                return true;
            }
        }
        return false;
    }

}

