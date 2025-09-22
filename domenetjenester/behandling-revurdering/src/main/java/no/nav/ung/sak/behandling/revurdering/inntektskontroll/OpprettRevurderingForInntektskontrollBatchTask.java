package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.*;


/**
 * Batchtask som starter kontroll av inntekt fra a-inntekt
 * <p>
 * Kjører den åttende i måneden kl 07:00.
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingForInntektskontrollBatchTask.TASKNAME, cronExpression = "0 0 7 8 * *", maxFailedRuns = 1)
public class OpprettRevurderingForInntektskontrollBatchTask implements ProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettRevurderingForInntektskontrollBatch";
    private ProsessTaskTjeneste prosessTaskTjeneste;

    OpprettRevurderingForInntektskontrollBatchTask() {
    }

    @Inject
    public OpprettRevurderingForInntektskontrollBatchTask(ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fom = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        var tom = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        ProsessTaskData kontrollTask = ProsessTaskData.forProsessTask(OpprettRevurderingForInntektskontrollTask.class);
        kontrollTask.setProperty(PERIODE_FOM, fom.format(DateTimeFormatter.ISO_LOCAL_DATE));
        kontrollTask.setProperty(PERIODE_TOM, tom.format(DateTimeFormatter.ISO_LOCAL_DATE));
        prosessTaskTjeneste.lagre(kontrollTask);
    }

}
