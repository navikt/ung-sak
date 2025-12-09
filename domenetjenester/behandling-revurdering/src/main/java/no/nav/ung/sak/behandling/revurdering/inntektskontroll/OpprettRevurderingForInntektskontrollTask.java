package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.*;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;


/**
 * Task som starter kontroll av inntekt fra a-inntekt
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingForInntektskontrollTask.TASKNAME, maxFailedRuns = 1)
public class OpprettRevurderingForInntektskontrollTask implements ProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettRevurderingForInntektskontroll";

    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";

    private static final Logger log = LoggerFactory.getLogger(OpprettRevurderingForInntektskontrollTask.class);

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FinnSakerForInntektkontroll finnRelevanteFagsaker;
    private CronExpression inntektskontrollCron;

    OpprettRevurderingForInntektskontrollTask() {
    }

    @Inject
    public OpprettRevurderingForInntektskontrollTask(
        ProsessTaskTjeneste prosessTaskTjeneste,
        FinnSakerForInntektkontroll finnRelevanteFagsaker,
        @KonfigVerdi(value = "INNTEKTSKONTROLL_CRON_EXPRESSION", defaultVerdi = "0 0 7 8 * *") String inntetskontrollCronString) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.finnRelevanteFagsaker = finnRelevanteFagsaker;
        this.inntektskontrollCron = CronExpression.create(inntetskontrollCronString);
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        ZonedDateTime starttidspunktForKontroll = inntektskontrollCron.nextTimeAfter(fom.atStartOfDay(ZoneId.systemDefault()));
        if (ZonedDateTime.now().isBefore(starttidspunktForKontroll)) {
            throw new IllegalStateException("Kan ikke kjøre inntektskontroll for periode før " + starttidspunktForKontroll + ". For periode med start " + fom);
        }
        final var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var fagsaker = finnRelevanteFagsaker.finnFagsaker(fom, tom);
        opprettProsessTaskerForÅSetteInntektrapporteringTilUtløpt(fagsaker, fom, tom);
        opprettKontrollProsessTasker(fagsaker, fom, tom);
    }


    private void opprettKontrollProsessTasker(List<Fagsak> fagsakerForKontroll, LocalDate fom, LocalDate tom) {
        ProsessTaskGruppe taskGruppeTilRevurderinger = new ProsessTaskGruppe();

        var revurderTasker = fagsakerForKontroll
            .stream()
            .map(fagsak -> {
                log.info("Oppretter revurdering for fagsak med saksnummer {} for inntektskontroll av periode {} - {}", fagsak.getSaksnummer(), fom, tom);

                ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
                tilVurderingTask.setFagsakId(fagsak.getId());
                tilVurderingTask.setProperty(PERIODER, fom + "/" + tom);
                tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT.getKode());
                return tilVurderingTask;
            }).toList();

        taskGruppeTilRevurderinger.addNesteParallell(revurderTasker);
        prosessTaskTjeneste.lagre(taskGruppeTilRevurderinger);
    }


    private void opprettProsessTaskerForÅSetteInntektrapporteringTilUtløpt(List<Fagsak> fagsakerForKontroll, LocalDate fom, LocalDate tom) {
        ProsessTaskGruppe taskGruppeTilRevurderinger = new ProsessTaskGruppe();

        var revurderTasker = fagsakerForKontroll
            .stream()
            .map(fagsak -> {
                log.info("Setter inntektrapportering til utløpt for fagsak med saksnummer {} og periode {} - {}", fagsak.getSaksnummer(), fom, tom);
                ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(SettOppgaveUtløptForInntektsrapporteringTask.class);
                tilVurderingTask.setAktørId(fagsak.getAktørId().getAktørId());
                tilVurderingTask.setProperty(SettOppgaveUtløptForInntektsrapporteringTask.PERIODE_FOM, fom.format(DateTimeFormatter.ISO_LOCAL_DATE));
                tilVurderingTask.setProperty(SettOppgaveUtløptForInntektsrapporteringTask.PERIODE_TOM, tom.format(DateTimeFormatter.ISO_LOCAL_DATE));
                return tilVurderingTask;
            }).toList();

        taskGruppeTilRevurderinger.addNesteParallell(revurderTasker);
        prosessTaskTjeneste.lagre(taskGruppeTilRevurderinger);
    }


}
