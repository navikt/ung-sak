package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.Virkedager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reduserer utbetaling dersom det er rapportert inntekt. Den aldersbestemte satsen reduseres med 66% av den rapporterte inntekten omregnet til dagsats.
 * Dagsats for rapporterte inntekter regnes ved å bruke antall virkedager i perioden den er innmeldt for.
 */
public class ReduserVedInntektVurderer implements UttakRegelVurderer {

    public static final BigDecimal REDUKSJONSGRAD = BigDecimal.valueOf(0.66);
    public static final BigDecimal HUNDRE_PROSENT = BigDecimal.valueOf(100);
    private LocalDateTimeline<Set<RapportertInntekt>> rapportertInntektTidslinje;
    private LocalDateTimeline<BigDecimal> aldersbestemtSatsTidslinje;

    public ReduserVedInntektVurderer(LocalDateTimeline<Set<RapportertInntekt>> rapportertInntektTidslinje,
                                     LocalDateTimeline<BigDecimal> aldersbestemtSatsTidslinje) {
        this.rapportertInntektTidslinje = rapportertInntektTidslinje;
        this.aldersbestemtSatsTidslinje = aldersbestemtSatsTidslinje;
    }

    @Override
    public UttakDelResultat vurder(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {

        // Lager tidslinje oppgitt inntekt pr dag (virkedag)
        final var dagsatsTidslinje = lagDagsatsTidslinje();
        // Beregner reduksjon ut i fra aldersbestemt sats
        final var redusertUtbetalingsgradTidslinje = aldersbestemtSatsTidslinje
            .intersection(tidslinjeTilVurdering)
            .combine(dagsatsTidslinje, ReduserVedInntektVurderer::beregnUtbetalingsgrad, LocalDateTimeline.JoinStyle.LEFT_JOIN);

        // Mapper utbetalingsgrad til uttaksperioder
        final var uttaksperioder = redusertUtbetalingsgradTidslinje.mapValue(UttakResultat::forInnvilgelse);

        final var dagsatsperiodeJsonArray = lagJsonArray(dagsatsTidslinje, "dagsats");
        final var utbetalingsgradPerioderJsonArray = lagJsonArray(redusertUtbetalingsgradTidslinje, "utbetalingsgrad");


        return new UttakDelResultat(uttaksperioder,
            tidslinjeTilVurdering.disjoint(redusertUtbetalingsgradTidslinje),
            Map.of("inntektdagsatsperioder", dagsatsperiodeJsonArray,
                "utbetalingsgradperioder", utbetalingsgradPerioderJsonArray)
        );
    }

    private static String lagJsonArray(LocalDateTimeline<BigDecimal> tidslinje, String navnVerdi) {
        final var dagsatsJsonString = tidslinje.stream().map(it -> """
                {
                    "navnVerdi": :verdi,
                    "periode": ":periode"
                }"""
                .replace("navnVerdi", navnVerdi)
                .replace(":verdi", it.getValue().setScale(2, RoundingMode.HALF_UP).toString())
                .replace(":periode", it.getFom().toString() + "/" + it.getTom().toString()))
            .collect(Collectors.joining(","));
        final var dagsatsperiodeJsonArray = "[" + dagsatsJsonString + "]";
        return dagsatsperiodeJsonArray;
    }


    private static ArrayList<UngdomsytelseUttakPeriode> mapTilUttaksperioder(LocalDateTimeline<BigDecimal> redusertUtbetalingsgradTidslinje) {
        return redusertUtbetalingsgradTidslinje
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(p.getValue(), DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom())))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static LocalDateSegment<BigDecimal> beregnUtbetalingsgrad(LocalDateInterval di, LocalDateSegment<BigDecimal> lhs, LocalDateSegment<BigDecimal> rhs) {
        return new LocalDateSegment<>(
            di, finnUtbetalingsgrad(lhs.getValue(), rhs == null ? BigDecimal.ZERO : rhs.getValue()));
    }

    private static BigDecimal finnUtbetalingsgrad(BigDecimal aldersbestemtSats, BigDecimal inntektDagsats) {
        return aldersbestemtSats
            .subtract(inntektDagsats.multiply(REDUKSJONSGRAD))
            .max(BigDecimal.ZERO)
            .divide(aldersbestemtSats, 10, RoundingMode.HALF_UP)
            .multiply(HUNDRE_PROSENT);
    }

    private LocalDateTimeline<BigDecimal> lagDagsatsTidslinje() {
        return rapportertInntektTidslinje
            .mapValue(v -> v.stream()
                .map(RapportertInntekt::beløp)
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO))
            .map(s -> {
                final var antallVirkedager = Virkedager.beregnAntallVirkedager(s.getFom(), s.getTom());
                if (antallVirkedager == 0) {
                    // Antagelsen er at ytelsen utbetales kun for virkedager og at inntekt rapporteres for hele måneder eller deler av måneder
                    // der bruker meldes inn eller ut av programmet. Dermed har reduksjon i helg ingen betydning. Vi setter derfor 0 i dagsats (ingen reduksjon).
                    return List.of(new LocalDateSegment<>(s.getLocalDateInterval(), BigDecimal.ZERO));
                } else {
                    return List.of(new LocalDateSegment<>(s.getLocalDateInterval(), s.getValue().divide(BigDecimal.valueOf(antallVirkedager), 10, RoundingMode.HALF_UP)));
                }
            });
    }
}
