package no.nav.ung.sak.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Dependent
public class RyddingAvInntektsrapporteringUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(RyddingAvInntektsrapporteringUtleder.class);
    private final MånedsvisTidslinjeUtleder ytelsesperiodeutleder;
    private final RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;
    private final CronExpression inntektskontrollCron;
    private final CronExpression inntektsrapporteringCron;


    @Inject
    public RyddingAvInntektsrapporteringUtleder(MånedsvisTidslinjeUtleder ytelsesperiodeutleder,
                                                RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder,
                                                @KonfigVerdi(value = "INNTEKTSKONTROLL_CRON_EXPRESSION", defaultVerdi = "0 0 7 8 * *") String inntektskontrollCronString,
                                                @KonfigVerdi(value = "INNTEKTSRAPPORTERING_CRON_EXPRESSION", defaultVerdi = "0 0 7 1 * *") String inntektsrapporteringCronString) {
        this.ytelsesperiodeutleder = ytelsesperiodeutleder;
        this.relevanteKontrollperioderUtleder = relevanteKontrollperioderUtleder;
        this.inntektskontrollCron = new CronExpression(inntektskontrollCronString);
        this.inntektsrapporteringCron = new CronExpression(inntektsrapporteringCronString);
    }

    public Optional<DatoIntervallEntitet> utledPerioderForRyddingAvRapporteringsoppgaver(BehandlingReferanse behandlingReferanse) {
        LocalDateTimeline<YearMonth> intiellePerioder = ytelsesperiodeutleder.finnInitielleMånedsvisPeriodisertePerioder(behandlingReferanse.getBehandlingId());
        LocalDateTimeline<Boolean> initielleRelevantePerioder = relevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(intiellePerioder);
        LocalDateTimeline<YearMonth> gjeldendePerioder = ytelsesperiodeutleder.finnMånedsvisPeriodisertePerioder(behandlingReferanse.getBehandlingId());
        LocalDateTimeline<Boolean> relevantePerioder = relevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(gjeldendePerioder);

        ZonedDateTime nå = ZonedDateTime.now();
        return finnBortfaltRapporteringsperiode(initielleRelevantePerioder, relevantePerioder, nå, inntektskontrollCron, inntektsrapporteringCron);
    }

    static Optional<DatoIntervallEntitet> finnBortfaltRapporteringsperiode(LocalDateTimeline<Boolean> initielleRelevantePerioder,
                                                                           LocalDateTimeline<Boolean> relevantePerioder,
                                                                           ZonedDateTime nå,
                                                                           CronExpression startKontrollCron,
                                                                           CronExpression startRapporteringCron) {
        LocalDateTimeline<Boolean> bortfalteRelevantePerioder = initielleRelevantePerioder.disjoint(relevantePerioder);

        ZonedDateTime nesteRapporteringsfrist = startKontrollCron.nextTimeAfter(nå);
        ZonedDateTime nesteRapporteringsstart = startRapporteringCron.nextTimeAfter(nå);
        if (nesteRapporteringsstart.isAfter(nesteRapporteringsfrist)) {
            // Har passert start for rapportering, men ikke fristen (mellom første og åttende i måneden)
            LocalDate fomNesteKontroll = nesteRapporteringsfrist.toLocalDate().minusMonths(1).withDayOfMonth(1);
            LocalDate tomNesteKontroll = nesteRapporteringsfrist.toLocalDate().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
            LocalDateTimeline<Boolean> bortfaltNesteKontroll = bortfalteRelevantePerioder.intersection(new LocalDateInterval(fomNesteKontroll, tomNesteKontroll)).compress();
            if (!bortfaltNesteKontroll.isEmpty()) {
                // Skal kun inneholde en periode
                LocalDateInterval periode = bortfaltNesteKontroll.getLocalDateIntervals().iterator().next();
                LOG.info("Utleder rydding av inntektsrapportering for periode {} - {}", periode.getFomDato(), periode.getTomDato());
                return Optional.of(DatoIntervallEntitet.fra(periode));
            }
        }

        return Optional.empty();
    }

}
