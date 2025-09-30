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
import java.util.stream.Collectors;

import static no.nav.ung.sak.behandling.revurdering.inntektskontroll.OpprettOppgaveForInntektsrapporteringTask.*;


/**
 * Oppretter oppgaver for rapportering av inntekt
 */
@ApplicationScoped
@ProsessTask(value = OpprettOppgaverForInntektsrapporteringTask.TASKNAME)
public class OpprettOppgaverForInntektsrapporteringTask implements ProsessTaskHandler {

    public static final String TASKNAME = "opprettOppgaverForInntektsrapportering";
    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";
    public static final String DRY_RUN = "dryrun";

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaverForInntektsrapporteringTask.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FinnSakerForInntektkontroll finnRelevanteFagsaker;

    OpprettOppgaverForInntektsrapporteringTask() {
    }

    @Inject
    public OpprettOppgaverForInntektsrapporteringTask(ProsessTaskTjeneste prosessTaskTjeneste, FinnSakerForInntektkontroll finnRelevanteFagsaker) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.finnRelevanteFagsaker = finnRelevanteFagsaker;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var fagsaker = finnRelevanteFagsaker.finnFagsaker(fom, tom);
        boolean dryrun = Boolean.parseBoolean(prosessTaskData.getPropertyValue(DRY_RUN));
        if (dryrun) {
            log.info("Kjører i dryrun modus, det vil si at ingen oppgaver blir opprettet");
            log.info("Resultat av dryrun for oppretting av oppgaver for inntektsrapportering for periode {} - {}: {} fagsaker funnet", fom, tom, fagsaker.size());
            log.info("Saker funnet ved dryrun for oppretting av oppgaver for inntektsrapportering for periode {} - {}: {}", fom, tom, fagsaker.stream().map(Fagsak::getSaksnummer).collect(Collectors.toSet()));
        } else {
            opprettProsessTask(fagsaker, fom, tom);
        }
    }


    private void opprettProsessTask(List<Fagsak> fagsakerForKontroll, LocalDate fom, LocalDate tom) {
        ProsessTaskGruppe taskGruppeTilRevurderinger = new ProsessTaskGruppe();

        var revurderTasker = fagsakerForKontroll
            .stream()
            .map(fagsak -> {
                log.info("Oppretter oppgave for inntektrappportering for fagsak {} for periode {} - {}", fagsak.getSaksnummer(), fom, tom);
                ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettOppgaveForInntektsrapporteringTask.class);
                tilVurderingTask.setAktørId(fagsak.getAktørId().getAktørId());
                tilVurderingTask.setSaksnummer(fagsak.getSaksnummer().getVerdi());
                tilVurderingTask.setProperty(OpprettOppgaveForInntektsrapporteringTask.PERIODE_FOM, fom.toString());
                tilVurderingTask.setProperty(OpprettOppgaveForInntektsrapporteringTask.PERIODE_TOM, tom.toString());
                tilVurderingTask.setProperty(OPPGAVE_REF, UUID.randomUUID().toString());
                return tilVurderingTask;
            }).toList();

        taskGruppeTilRevurderinger.addNesteParallell(revurderTasker);
        prosessTaskTjeneste.lagre(taskGruppeTilRevurderinger);
    }


}
