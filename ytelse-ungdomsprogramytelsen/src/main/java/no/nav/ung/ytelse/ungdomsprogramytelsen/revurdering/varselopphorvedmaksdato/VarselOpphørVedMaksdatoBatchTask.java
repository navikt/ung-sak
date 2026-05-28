package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.BatchProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Batchtask som varsler deltakere om opphør ved maksdato 3 uker før maksdato.
 * <p>
 * Kjører hver dag kl 07:30.
 */
@ApplicationScoped
@ProsessTask(value = VarselOpphørVedMaksdatoBatchTask.TASKNAME, maxFailedRuns = 1)
public class VarselOpphørVedMaksdatoBatchTask implements BatchProsessTaskHandler {

    public static final String TASKNAME = "batch.varselOpphorVedMaksdato";
    private static final Logger log = LoggerFactory.getLogger(VarselOpphørVedMaksdatoBatchTask.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private boolean varselOpphørVedMaksdatoEnabled;

    VarselOpphørVedMaksdatoBatchTask() {
        // for CDI proxy
    }

    @Inject
    public VarselOpphørVedMaksdatoBatchTask(ProsessTaskTjeneste prosessTaskTjeneste,
                                            @KonfigVerdi(value = "VARSEL_OPPHOR_VED_MAKSDATO_ENABLED", required = false, defaultVerdi = "false") boolean varselOpphørVedMaksdatoEnabled) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.varselOpphørVedMaksdatoEnabled = varselOpphørVedMaksdatoEnabled;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (!varselOpphørVedMaksdatoEnabled) {
            log.info("Varsel opphør ved maksdato er deaktivert via konfigurasjon");
            return;
        }

        List<ProsessTaskData> feiletTask = prosessTaskTjeneste.finnAlle(VarselOpphørVedMaksdatoTask.TASKNAME, ProsessTaskStatus.FEILET).stream().filter(it -> it.getSaksnummer() == null).toList();
        List<ProsessTaskData> klarTask = prosessTaskTjeneste.finnAlle(VarselOpphørVedMaksdatoTask.TASKNAME, ProsessTaskStatus.KLAR).stream().filter(it -> it.getSaksnummer() == null).toList();
        List<ProsessTaskData> vetoTask = prosessTaskTjeneste.finnAlle(VarselOpphørVedMaksdatoTask.TASKNAME, ProsessTaskStatus.VETO).stream().filter(it -> it.getSaksnummer() == null).toList();
        if (!feiletTask.isEmpty() || !klarTask.isEmpty() || !vetoTask.isEmpty()) {
            return;
        }

        ProsessTaskData taskData = ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class);
        prosessTaskTjeneste.lagre(taskData);
    }

    @Override
    public CronExpression getCron() {
        return CronExpression.create("0 30 7 * * *");
    }
}

