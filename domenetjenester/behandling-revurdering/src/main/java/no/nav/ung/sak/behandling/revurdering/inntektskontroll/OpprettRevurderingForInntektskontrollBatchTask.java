package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.function.Predicate;


/**
 * Batchtask som starter kontroll av inntekt fra a-inntekt
 * <p>
 * Kjører den åttende i måneden kl 07:00.
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingForInntektskontrollBatchTask.TASKNAME, maxFailedRuns = 1)
public class OpprettRevurderingForInntektskontrollBatchTask implements BatchProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettRevurderingForInntektskontrollBatch";

    private static final Logger log = LoggerFactory.getLogger(OpprettRevurderingForInntektskontrollBatchTask.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;

    OpprettRevurderingForInntektskontrollBatchTask() {
    }

    @Inject
    public OpprettRevurderingForInntektskontrollBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public CronExpression getCron() {



        return CronExpression.create("0 0 7 8 * *");
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fom = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        var tom = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());


        List<ProsessTaskData> feiletTask = prosessTaskTjeneste.finnAlle(OpprettRevurderingForInntektskontrollTask.TASKNAME, ProsessTaskStatus.FEILET).stream().filter(gjelderSammePeriode(fom)).toList();
        List<ProsessTaskData> klarTask = prosessTaskTjeneste.finnAlle(OpprettRevurderingForInntektskontrollTask.TASKNAME, ProsessTaskStatus.KLAR).stream().filter(gjelderSammePeriode(fom)).toList();
        List<ProsessTaskData> vetoTask = prosessTaskTjeneste.finnAlle(OpprettRevurderingForInntektskontrollTask.TASKNAME, ProsessTaskStatus.VETO).stream().filter(gjelderSammePeriode(fom)).toList();
        if (!feiletTask.isEmpty() || !klarTask.isEmpty() || !vetoTask.isEmpty()) {
            // Hvis det finnes noen task i noen av disse statusene, så betyr det at de enten kjører, eller skal kjøres.
            // Vi ønsker ikke å opprette duplikater av disse.
            log.info("Kontroll av inntekt for perioden {} - {} er allerede opprettet som task. Feilet: {}, Klar: {}, Veto: {}. Oppretter ikke duplikat", fom, tom, feiletTask.size(), klarTask.size(), vetoTask.size());
            return;
        }

        ProsessTaskData kontrollTask = ProsessTaskData.forProsessTask(OpprettRevurderingForInntektskontrollTask.class);
        kontrollTask.setProperty(OpprettRevurderingForInntektskontrollTask.PERIODE_FOM, fom.format(DateTimeFormatter.ISO_LOCAL_DATE));
        kontrollTask.setProperty(OpprettRevurderingForInntektskontrollTask.PERIODE_TOM, tom.format(DateTimeFormatter.ISO_LOCAL_DATE));
        prosessTaskTjeneste.lagre(kontrollTask);
    }

    private static Predicate<ProsessTaskData> gjelderSammePeriode(LocalDate fom) {
        return it -> it.getPropertyValue(OpprettRevurderingForInntektskontrollTask.PERIODE_FOM).equals(fom.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

}
