package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
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

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaverForInntektsrapporteringBatchTask.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FinnSakerForInntektkontroll finnRelevanteFagsaker;

    OpprettOppgaverForInntektsrapporteringBatchTask() {
    }

    @Inject
    public OpprettOppgaverForInntektsrapporteringBatchTask(ProsessTaskTjeneste prosessTaskTjeneste, FinnSakerForInntektkontroll finnRelevanteFagsaker) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.finnRelevanteFagsaker = finnRelevanteFagsaker;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fom = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        var tom = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        final var fagsaker = finnRelevanteFagsaker.finnFagsaker(fom, tom);
        opprettProsessTask(fagsaker, fom, tom);
    }


    private void opprettProsessTask(List<Fagsak> fagsakerForKontroll, LocalDate fom, LocalDate tom) {
        ProsessTaskGruppe taskGruppeTilRevurderinger = new ProsessTaskGruppe();

        var revurderTasker = fagsakerForKontroll
            .stream()
            .map(fagsak -> {
                log.info("Oppretter oppgave for inntektrappportering for fagsak {} for periode {} - {}", fagsak.getSaksnummer(), fom, tom);
                ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettOppgaveForInntektsrapporteringTask.class);
                tilVurderingTask.setAktørId(fagsak.getAktørId().getAktørId());
                tilVurderingTask.setFagsakId(fagsak.getId());
                tilVurderingTask.setProperty(PERIODE_FOM, fom.toString());
                tilVurderingTask.setProperty(PERIODE_TOM, tom.toString());
                tilVurderingTask.setProperty(OPPGAVE_REF, UUID.randomUUID().toString());
                return tilVurderingTask;
            }).toList();

        taskGruppeTilRevurderinger.addNesteParallell(revurderTasker);
        prosessTaskTjeneste.lagre(taskGruppeTilRevurderinger);
    }


}
