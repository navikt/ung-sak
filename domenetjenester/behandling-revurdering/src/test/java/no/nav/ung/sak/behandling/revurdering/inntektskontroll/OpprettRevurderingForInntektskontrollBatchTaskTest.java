package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpprettRevurderingForInntektskontrollBatchTaskTest {

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private OpprettRevurderingForInntektskontrollBatchTask batchTask;

    @BeforeEach
    void setUp() {
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        batchTask = new OpprettRevurderingForInntektskontrollBatchTask(prosessTaskTjeneste, "0 0 7 8 * *");

        when(prosessTaskTjeneste.finnAlle(eq(OpprettRevurderingForInntektskontrollTask.TASKNAME), any(ProsessTaskStatus.class)))
            .thenReturn(List.of());
    }

    @Test
    void skal_sette_periode_fom_og_tom_på_child_task() {
        var forrigeMåned = YearMonth.now().minusMonths(1);
        var forventetFom = forrigeMåned.atDay(1);
        var forventetTom = forrigeMåned.atEndOfMonth();

        batchTask.doTask(new ProsessTaskData(OpprettRevurderingForInntektskontrollBatchTask.TASKNAME));

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());

        var childTask = captor.getValue();
        assertThat(childTask.getTaskType()).isEqualTo(OpprettRevurderingForInntektskontrollTask.TASKNAME);
        assertThat(childTask.getPropertyValue(OpprettRevurderingForInntektskontrollTask.PERIODE_FOM))
            .isEqualTo(forventetFom.format(DateTimeFormatter.ISO_LOCAL_DATE));
        assertThat(childTask.getPropertyValue(OpprettRevurderingForInntektskontrollTask.PERIODE_TOM))
            .isEqualTo(forventetTom.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @Test
    void skal_ikke_opprette_duplikat_når_feilet_task_for_samme_periode_finnes() {
        var fom = YearMonth.now().minusMonths(1).atDay(1);
        var eksisterendeTask = new ProsessTaskData(OpprettRevurderingForInntektskontrollTask.TASKNAME);
        eksisterendeTask.setProperty(OpprettRevurderingForInntektskontrollTask.PERIODE_FOM, fom.format(DateTimeFormatter.ISO_LOCAL_DATE));

        when(prosessTaskTjeneste.finnAlle(OpprettRevurderingForInntektskontrollTask.TASKNAME, ProsessTaskStatus.FEILET))
            .thenReturn(List.of(eksisterendeTask));

        batchTask.doTask(new ProsessTaskData("batch.opprettRevurderingForInntektskontrollBatch"));

        verify(prosessTaskTjeneste, org.mockito.Mockito.never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_feile_når_eksisterende_task_mangler_periode_property() {
        // Regresjonstest: eksisterende feilede tasks kan mangle PERIODE_FOM (bug fikset i samme endring
        // som denne testen), erDuplikat skal håndtere det uten NPE.
        var eksisterendeTaskUtenPeriode = new ProsessTaskData(OpprettRevurderingForInntektskontrollTask.TASKNAME);

        when(prosessTaskTjeneste.finnAlle(OpprettRevurderingForInntektskontrollTask.TASKNAME, ProsessTaskStatus.FEILET))
            .thenReturn(List.of(eksisterendeTaskUtenPeriode));

        batchTask.doTask(new ProsessTaskData("batch.opprettRevurderingForInntektskontrollBatch"));

        verify(prosessTaskTjeneste).lagre(any(ProsessTaskData.class));
    }
}
