package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static no.nav.ung.sak.behandling.revurdering.inntektskontroll.OpprettOppgaveForInntektsrapporteringTask.*;


/**
 * Batchtask som oppretter oppgaver for rapportering av inntekt
 * <p>
 * Kjører den første i måneden kl 07:00.
 */
@ApplicationScoped
@ProsessTask(value = OpprettOppgaverForInntektsrapporteringBatchTask.TASKNAME, cronExpression = "0 0 7 1 * *", maxFailedRuns = 1)
public class OpprettOppgaverForInntektsrapporteringBatchTask implements ProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettOppgaverForInntektsrapporteringBatch";


    private ProsessTaskTjeneste prosessTaskTjeneste;

    OpprettOppgaverForInntektsrapporteringBatchTask() {
    }

    @Inject
    public OpprettOppgaverForInntektsrapporteringBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fom = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        var tom = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        ProsessTaskData opprettOppgaverTask = ProsessTaskData.forProsessTask(OpprettOppgaverForInntektsrapporteringTask.class);
        opprettOppgaverTask.setProperty(OpprettOppgaverForInntektsrapporteringTask.PERIODE_FOM, fom.format(DateTimeFormatter.ISO_LOCAL_DATE));
        opprettOppgaverTask.setProperty(OpprettOppgaverForInntektsrapporteringTask.PERIODE_TOM, tom.format(DateTimeFormatter.ISO_LOCAL_DATE));
        opprettOppgaverTask.setProperty(OpprettOppgaverForInntektsrapporteringTask.DRY_RUN, Boolean.FALSE.toString());
        prosessTaskTjeneste.lagre(opprettOppgaverTask);
    }


}
