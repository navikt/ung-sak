package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.etterlysning.MidlertidigOppgaveDelegeringTjeneste;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;


/**
 * Task som oppretter oppgave for inntektsrapportering.
 */
@ApplicationScoped
@ProsessTask(value = OpprettOppgaveForInntektsrapporteringTask.TASKNAME)
public class OpprettOppgaveForInntektsrapporteringTask implements ProsessTaskHandler {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");
    private static final Logger LOG = LoggerFactory.getLogger(OpprettOppgaveForInntektsrapporteringTask.class);


    public static final String TASKNAME = "inntektsrapportering.opprettOppgave";

    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";
    public static final String OPPGAVE_REF = "oppgave_ref";

    private MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    private CronExpression inntektskontrollCronExpression;


    OpprettOppgaveForInntektsrapporteringTask() {
    }

    @Inject
    public OpprettOppgaveForInntektsrapporteringTask(MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste,
                                                     FagsakRepository fagsakRepository,
                                                     BehandlingRepository behandlingRepository,
                                                     MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder,
                                                     @KonfigVerdi(value = "INNTEKTSKONTROLL_CRON_EXPRESSION", defaultVerdi = "0 0 7 8 * *") String inntetskontrollCronString) {

        this.delegeringTjeneste = delegeringTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
        this.inntektskontrollCronExpression = CronExpression.create(inntetskontrollCronString);
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var aktørId = new AktørId(prosessTaskData.getAktørId());
        final var fom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_FOM), DateTimeFormatter.ISO_LOCAL_DATE);
        final var tom = LocalDate.parse(prosessTaskData.getPropertyValue(PERIODE_TOM), DateTimeFormatter.ISO_LOCAL_DATE);
        if (prosessTaskData.getSaksnummer() != null) {
            logContext(new Saksnummer(prosessTaskData.getSaksnummer()));
            LOG.info("Oppretter oppgave for inntektsrapportering for periode={}", new Periode(fom, tom));
        }
        boolean harIkkeYtelseIHelePerioden = harYtelseIDelAvPerioden(aktørId, fom, tom);
        var nesteKontrolltidspunkt = inntektskontrollCronExpression.nextTimeAfter(fom.atStartOfDay(ZoneId.systemDefault()));
        var frist = nesteKontrolltidspunkt.toLocalDateTime().toLocalDate().atStartOfDay();
        delegeringTjeneste.opprettOppgave(new OpprettOppgaveDto(
            aktørId,
            UUID.fromString(prosessTaskData.getPropertyValue(OPPGAVE_REF)),
            new InntektsrapporteringOppgavetypeDataDto(fom, tom, harIkkeYtelseIHelePerioden),
            frist
        ));
    }

    /** Sjekker om bruker ikke har ytelse i hele perioden, men kun i deler av perioden
     * @param aktørId AktlørId til bruker
     * @param fom Første dag i måned til rapportering
     * @param tom Siste dag i måned til rapportering
     * @return
     */
    private boolean harYtelseIDelAvPerioden(AktørId aktørId, LocalDate fom, LocalDate tom) {
        List<Fagsak> fagsaker = fagsakRepository.hentForBruker(aktørId);
        if (fagsaker.isEmpty()) {
            throw new IllegalStateException("Fant ikke fagsak for aktørId=" + aktørId.getId());
        } else if (fagsaker.size() != 1) {
            throw new IllegalStateException("Forventer kun en fagsak for aktørId=" + aktørId.getId() + ", men fant flere: " + fagsaker.stream().map(Fagsak::getSaksnummer).toList());
        }
        var fagsak = fagsaker.get(0);

        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow(() -> new IllegalStateException("Fant ikke behandling"));
        LocalDateTimeline<YearMonth> månedsvisTidslinje = månedsvisTidslinjeUtleder.finnMånedsvisPeriodisertePerioder(sisteBehandling.getId());
        LocalDateInterval månedForRapportering = new LocalDateInterval(fom, tom);
        return overlapperPeriodeDelvisMedProgramtidslinje(månedForRapportering, månedsvisTidslinje);
    }

    private static <T> boolean overlapperPeriodeDelvisMedProgramtidslinje(LocalDateInterval periode, LocalDateTimeline<T> programtidslinje) {
        LocalDateTimeline<Boolean> periodeSomTidslinje = new LocalDateTimeline<>(periode, true);
        LocalDateTimeline<T> overlapp = programtidslinje.intersection(periode);
        LocalDateTimeline<Boolean> periodeEtterFjernetOverlapp = periodeSomTidslinje.disjoint(overlapp);
        return !periodeEtterFjernetOverlapp.isEmpty();
    }
    /** log mdc cleares automatisk når task har kjørt, så trenger ikke kalle clearLogContext. */
    public static void logContext(Saksnummer saksnummer) {
        LOG_CONTEXT.add("saksnummer", saksnummer);
    }


}
