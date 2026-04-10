package no.nav.ung.sak.web.app.aktivitetspenger;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.AktivitetspengerSatsPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.AktivitetspengerSatsType;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ytelse.AktivitetspengerUtbetaltMånedDto;
import no.nav.ung.sak.web.app.ungdomsytelse.BehandlingAvsluttetTidspunkt;
import no.nav.ung.sak.web.app.ungdomsytelse.UtbetalingstatusUtleder;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MånedsvisningDtoMapper {

    public static List<AktivitetspengerUtbetaltMånedDto> mapSatsOgUtbetalingPrMåned(
        BehandlingAvsluttetTidspunkt aktuellAvsluttetTid,
        LocalDateTimeline<YearMonth> månedsvisPeriodisering,
        LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelseTidslinje,
        LocalDateTimeline<KontrollerteInntekter> kontrollertInntektTidslinje,
        LocalDateTimeline<AktivitetspengerSatser> satsTidslinje,
        Map<BehandlingAvsluttetTidspunkt, LocalDateTimeline<TilkjentYtelseVerdi>> tidslinjeMap) {

        var statusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(aktuellAvsluttetTid, tidslinjeMap, LocalDate.now());
        final var månederMedYtelse = månedsvisPeriodisering.intersection(tilkjentYtelseTidslinje.mapValue(it -> true).compress());
        return månederMedYtelse.toSegments().stream().map(måned -> {
            final var tilkjentYtelseForMåned = tilkjentYtelseTidslinje.intersection(måned.getLocalDateInterval());
            final var kontrollertInntektForMåned = kontrollertInntektTidslinje.intersection(måned.getLocalDateInterval());
            final var satsperioder = mapSatsperioderForMåned(måned, satsTidslinje);
            final var antallYtelsesdagerIMåned = finnAntallDagerForSatsperioder(satsperioder);
            final var utbetaltBeløp = finnUtbetaltBeløp(tilkjentYtelseForMåned);
            final var reduksjon = finnReduksjon(tilkjentYtelseForMåned);
            final var rapportertInntekt = finnRapportertInntekt(kontrollertInntektForMåned);
            final var reduksjonsgrunnlag = finnReduksjonsgrunnlag(måned, rapportertInntekt, antallYtelsesdagerIMåned);
            final var utbetalingStatus = statusTidslinje.getSegment(måned.getLocalDateInterval()).getValue();
            boolean slutterYtelseFørMånedsslutt = måned.getTom().isBefore(måned.getTom().with(TemporalAdjusters.lastDayOfMonth()));

            return new AktivitetspengerUtbetaltMånedDto(
                slutterYtelseFørMånedsslutt,
                måned.getValue(),
                satsperioder,
                antallYtelsesdagerIMåned,
                rapportertInntekt.orElse(null),
                reduksjonsgrunnlag.orElse(null),
                reduksjon,
                utbetaltBeløp,
                utbetalingStatus);
        }).toList();
    }

    private static Optional<BigDecimal> finnReduksjonsgrunnlag(LocalDateSegment<YearMonth> måned, Optional<BigDecimal> rapportertInntekt, Integer antallYtelsesdagerIMåned) {
        final var totaltAntallVirkedagerDagerIMåned = Virkedager.beregnAntallVirkedager(måned.getFom(), måned.getTom().with(TemporalAdjusters.lastDayOfMonth()));
        return rapportertInntekt.map(it -> BigDecimal.valueOf(antallYtelsesdagerIMåned)
            .divide(BigDecimal.valueOf(totaltAntallVirkedagerDagerIMåned), 10, RoundingMode.HALF_UP)
            .multiply(it).setScale(0, RoundingMode.HALF_UP));
    }

    private static Integer finnAntallDagerForSatsperioder(List<AktivitetspengerSatsPeriodeDto> satsperioder) {
        return satsperioder.stream().map(AktivitetspengerSatsPeriodeDto::antallDager)
            .reduce(Integer::sum)
            .orElse(0);
    }

    private static List<AktivitetspengerSatsPeriodeDto> mapSatsperioderForMåned(LocalDateSegment<YearMonth> måned, LocalDateTimeline<AktivitetspengerSatser> satsTidslinje) {
        return satsTidslinje.intersection(måned.getLocalDateInterval()).toSegments().stream()
            .map(it -> mapTilSatsperiode(it.getLocalDateInterval(), it.getValue()))
            .toList();
    }

    private static BigDecimal finnUtbetaltBeløp(LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelseForMåned) {
        return tilkjentYtelseForMåned.toSegments().stream().map(LocalDateSegment::getValue)
            .map(TilkjentYtelseVerdi::tilkjentBeløp)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private static Optional<BigDecimal> finnRapportertInntekt(LocalDateTimeline<KontrollerteInntekter> kontrollertInntektForMåned) {
        return kontrollertInntektForMåned.toSegments().stream().map(LocalDateSegment::getValue)
            .map(KontrollerteInntekter::inntekt)
            .reduce(BigDecimal::add);
    }

    private static BigDecimal finnReduksjon(LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelseForMåned) {
        return tilkjentYtelseForMåned.toSegments().stream().map(LocalDateSegment::getValue)
            .map(TilkjentYtelseVerdi::reduksjon)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private static AktivitetspengerSatsPeriodeDto mapTilSatsperiode(LocalDateInterval periode, AktivitetspengerSatser satser) {
        var beregnetSats = satser.hentBeregnetSats();
        return new AktivitetspengerSatsPeriodeDto(
            periode.getFomDato(),
            periode.getTomDato(),
            beregnetSats.dagsats().setScale(0, RoundingMode.HALF_UP),
            satser.satsGrunnlag().grunnbeløpFaktor(),
            satser.satsGrunnlag().grunnbeløp(),
            utledSatsType(satser),
            satser.satsGrunnlag().antallBarn(),
            beregnetSats.dagsatsBarnetillegg(),
            Virkedager.beregnAntallVirkedager(periode.getFomDato(), periode.getTomDato()));
    }

    private static AktivitetspengerSatsType utledSatsType(AktivitetspengerSatser satser) {
        return switch (satser.utledGrunnsatsBenyttet()) {
            case BEREGNINGSGRUNNLAG -> AktivitetspengerSatsType.BEREGNINGSGRUNNLAG;
            case MINSTEYTELSE -> switch (satser.satsGrunnlag().satsType()) {
                case HØY -> AktivitetspengerSatsType.HØY;
                case LAV -> AktivitetspengerSatsType.LAV;
            };
        };
    }
}
