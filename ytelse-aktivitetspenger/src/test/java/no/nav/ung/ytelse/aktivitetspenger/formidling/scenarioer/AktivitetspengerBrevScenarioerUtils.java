package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.sats.BarnetilleggSatsTidslinje;
import no.nav.ung.sak.behandlingslager.ytelse.sats.GrunnbeløpfaktorTidslinje;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.SatsOgGrunnbeløpfaktor;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.sak.ytelse.BeregnetSats;
import no.nav.ung.sak.ytelse.InntektsreduksjonKonfigurasjon;
import no.nav.ung.sak.ytelse.ReduksjonBeregner;
import no.nav.ung.sak.ytelse.TilkjentYtelseBeregner;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BeregningInput;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregnytelse.TotalBeløpForPeriodeMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;

public class AktivitetspengerBrevScenarioerUtils {

    public static final String DEFAULT_NAVN = "Ung Testesen";

    public static AktivitetspengerSatsGrunnlag.Builder lavSatsBuilder(LocalDate fom) {
        return lavSatsMedBarnBuilder(fom, 0);
    }

    public static AktivitetspengerSatsGrunnlag.Builder lavSatsMedBarnBuilder(LocalDate fom, int antallBarn) {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.LAV);
        var barneTillegg = BarnetilleggSatsTidslinje.BARNETILLEGG_DAGSATS.getSegment(new LocalDateInterval(fom, fom)).getValue();
        BigDecimal g = hentGrunnbeløpFor(fom);
        return AktivitetspengerSatsGrunnlag.builder()
            .medGrunnbeløp(g)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(antallBarn)
            .medBarnetilleggDagsats(beregnDagsatsBarnetillegg(antallBarn, barneTillegg).intValue());
    }

    public static AktivitetspengerSatsGrunnlag.Builder høySatsBuilder(LocalDate fom) {
        return høySatsBuilderMedBarn(fom, 0);
    }

    public static AktivitetspengerSatsGrunnlag.Builder høySatsBuilderMedBarn(LocalDate fom, int antallBarn) {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.HØY);
        var barneTillegg = BarnetilleggSatsTidslinje.BARNETILLEGG_DAGSATS.getSegment(new LocalDateInterval(fom, fom)).getValue();
        var g = hentGrunnbeløpFor(fom);
        return AktivitetspengerSatsGrunnlag.builder()
            .medGrunnbeløp(g)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(antallBarn)
            .medBarnetilleggDagsats(beregnDagsatsBarnetillegg(antallBarn, barneTillegg).intValue());
    }

    public static Beregningsgrunnlag lagBeregningsgrunnlag(LocalDate skjæringstidspunkt) {
        var input = new BeregningInput(new Beløp(0), new Beløp(0), new Beløp(0), skjæringstidspunkt, Year.from(skjæringstidspunkt).minusYears(1));
        return new Beregningsgrunnlag(input, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "test-sporing");
    }

    public static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder(
        LocalDateTimeline<AktivitetspengerSatsGrunnlag> satser, LocalDateInterval tilkjentPeriode) {

        LocalDateTimeline<YearMonth> ytelseTidslinje = splitPrMåned(new LocalDateTimeline<>(tilkjentPeriode, true))
            .map(s -> List.of(new LocalDateSegment<>(s.getLocalDateInterval(), YearMonth.of(s.getFom().getYear(), s.getFom().getMonthValue()))));
        LocalDateTimeline<BeregnetSats> beregnetSats = TotalBeløpForPeriodeMapper.mapSatserTilTotalbeløpForPerioder(satser, ytelseTidslinje);
        return beregnetSats.combine(LocalDateTimeline.<BigDecimal>empty(),
            (s, lhs, rhs) -> {
                var beregner = new ReduksjonBeregner(new KontrollerteInntekter(BigDecimal.ZERO, BigDecimal.ZERO), new InntektsreduksjonKonfigurasjon(new BigDecimal("0.66"), new BigDecimal("0.66")), s);
                return new LocalDateSegment<>(s, TilkjentYtelseBeregner.beregn(s, lhs.getValue(), beregner).verdi());
            },
            LocalDateTimeline.JoinStyle.LEFT_JOIN
        );
    }

    private static <T> LocalDateTimeline<T> splitPrMåned(LocalDateTimeline<T> tidslinje) {
        return tidslinje.splitAtRegular(tidslinje.getMinLocalDate().withDayOfMonth(1), tidslinje.getMaxLocalDate(), Period.ofMonths(1));
    }

    private static BigDecimal hentGrunnbeløpFor(LocalDate fom) {
        return GrunnbeløpTidslinje.hentTidslinje().getSegment(new LocalDateInterval(fom, fom)).getValue().verdi();
    }

    private static BigDecimal beregnDagsatsBarnetillegg(int antallBarn, BigDecimal barneTillegg) {
        return barneTillegg.multiply(BigDecimal.valueOf(antallBarn));
    }

    private static SatsOgGrunnbeløpfaktor hentSatstypeOgGrunnbeløp(Sats sats) {
        return GrunnbeløpfaktorTidslinje.hentGrunnbeløpfaktorTidslinjeFor(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), sats)
        ))).stream().findFirst().orElseThrow().getValue();
    }
}




