package no.nav.ung.sak.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.impl.cron.CronExpression;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;

@Dependent
public class ManglendeKontrollperioderTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ManglendeKontrollperioderTjeneste.class);

    private final CronExpression inntektskontrollCron;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;

    @Inject
    public ManglendeKontrollperioderTjeneste(MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder,
                                             @KonfigVerdi(value = "INNTEKTSKONTROLL_CRON_EXPRESSION", defaultVerdi = "0 0 7 8 * *") String inntektskontrollCronString,
                                             TilkjentYtelseRepository tilkjentYtelseRepository, RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder) {
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
        this.inntektskontrollCron = CronExpression.create(inntektskontrollCronString);
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.relevanteKontrollperioderUtleder = relevanteKontrollperioderUtleder;
    }

    /**
     * Lager prosesstaskdata for revurdering grunnet manglende kontroll av inntekt
     * Dette kan skje dersom programperioden er flyttet langt nok tilbake i tid eller at første søknad er sendt inn etter at rapporteringsfristen for andre måned er passert.
     *
     * @param behandlingId BehandlingId
     * @return
     */
    public NavigableSet<DatoIntervallEntitet> finnPerioderForManglendeKontroll(Long behandlingId) {
        final var månedsvisYtelsestidslinje = månedsvisTidslinjeUtleder.finnMånedsvisPeriodisertePerioder(behandlingId);
        final var påkrevdKontrollTidslinje = relevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(månedsvisYtelsestidslinje);
        final var passertRapporteringsfristTidslinje = finnPerioderMedPassertRapporteringsfrist();
        final var markertForKontrollTidslinje = finnPerioderMarkertForKontroll(behandlingId);
        var utførtKontrollTidslinje = finnPerioderSomErKontrollertITidligereBehandlinger(behandlingId);
        final var manglendeKontrollTidslinje = påkrevdKontrollTidslinje.disjoint(utførtKontrollTidslinje).disjoint(markertForKontrollTidslinje).intersection(passertRapporteringsfristTidslinje);
        if (manglendeKontrollTidslinje.isEmpty()) {
            return new TreeSet<>();
        }
        return splittPåMåneder(manglendeKontrollTidslinje).stream()
            .map(DatoIntervallEntitet::fra)
            .collect(Collectors.toCollection(TreeSet::new));

    }


    private static Set<LocalDateInterval> splittPåMåneder(LocalDateTimeline<Boolean> manglendeKontrollTidslinje) {
        return manglendeKontrollTidslinje.compress()
            .splitAtRegular(manglendeKontrollTidslinje.getMinLocalDate().withDayOfMonth(1), manglendeKontrollTidslinje.getMaxLocalDate(), Period.ofMonths(1))
            .getLocalDateIntervals();
    }

    private LocalDateTimeline<Boolean> finnPerioderMarkertForKontroll(Long behandlingId) {
        return relevanteKontrollperioderUtleder.utledPerioderForKontrollAvInntekt(behandlingId);
    }

    private LocalDateTimeline<Boolean> finnPerioderSomErKontrollertITidligereBehandlinger(Long behandlingId) {
        return tilkjentYtelseRepository.hentKontrollerInntektTidslinje(behandlingId).mapValue(it -> true);
    }

    private LocalDateTimeline<Boolean> finnPerioderMedPassertRapporteringsfrist() {
        return new LocalDateTimeline<>(TIDENES_BEGYNNELSE, getTomDatoForPassertRapporteringsfrist(), true);
    }

    private LocalDate getTomDatoForPassertRapporteringsfrist() {
        final var nå = ZonedDateTime.now();
        ZonedDateTime forrigeKontrollTidspunkt = inntektskontrollCron.nextTimeAfter(nå).minusMonths(1);
        return forrigeKontrollTidspunkt.toLocalDate().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
    }

}
