package no.nav.ung.sak.web.app.ungdomsytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPerioder;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.kontrakt.ungdomsytelse.beregning.UngdomsytelseSatsPeriodeDto;
import no.nav.ung.sak.kontrakt.ungdomsytelse.ytelse.UngdomsytelseUtbetaltMånedDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MånedsvisningDtoMapper {

    static List<UngdomsytelseUtbetaltMånedDto> mapSatsOgUtbetalingPrMåned(BehandlingAvsluttetTidspunkt aktuellAvsluttetTid,
                                                                          LocalDateTimeline<YearMonth> månedsvisPeriodisering,
                                                                          LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelseTidslinje,
                                                                          LocalDateTimeline<BigDecimal> kontrollertInntektTidslinje,
                                                                          UngdomsytelseSatsPerioder perioder,
                                                                          Map<BehandlingAvsluttetTidspunkt, LocalDateTimeline<TilkjentYtelseVerdi>> tidslinjeMap) {
        var statusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(aktuellAvsluttetTid, tidslinjeMap, LocalDate.now());
        final var månederMedYtelse = månedsvisPeriodisering.intersection(tilkjentYtelseTidslinje.mapValue(it -> true).compress());
        return månederMedYtelse.toSegments().stream().map(måned -> {
            final var tilkjentYtelseForMåned = tilkjentYtelseTidslinje.intersection(måned.getLocalDateInterval());
            final var kontrollertInntektForMåned = kontrollertInntektTidslinje.intersection(måned.getLocalDateInterval());
            final var satsperioder = mapSatsperioderForMåned(måned, perioder);
            final var antallDagerIMåned = finnAntallDagerForSatsperioder(satsperioder);
            final var utbetaltBeløp = finnUtbetaltBeløp(tilkjentYtelseForMåned);
            final var reduksjon = finnReduksjon(tilkjentYtelseForMåned);
            final var rapportertInntekt = finnRapportertInntekt(kontrollertInntektForMåned);
            final var utbetalingStatus = statusTidslinje.getSegment(måned.getLocalDateInterval()).getValue();
            return new UngdomsytelseUtbetaltMånedDto(
                måned.getValue(),
                satsperioder,
                antallDagerIMåned,
                rapportertInntekt.orElse(null),
                reduksjon,
                utbetaltBeløp,
                utbetalingStatus);
        }).toList();
    }

    private static Integer finnAntallDagerForSatsperioder(List<UngdomsytelseSatsPeriodeDto> satsperioder) {
        return satsperioder.stream().map(UngdomsytelseSatsPeriodeDto::antallDager)
            .reduce(Integer::sum)
            .orElse(0);
    }

    private static List<UngdomsytelseSatsPeriodeDto> mapSatsperioderForMåned(LocalDateSegment<YearMonth> måned, UngdomsytelseSatsPerioder perioder) {
        final var satsTidslinje = perioder.getPerioder()
            .stream()
            .map(it -> new LocalDateTimeline<>(it.getPeriode().toLocalDateInterval(), it))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
        final var overlappendeSatsperioder = satsTidslinje.intersection(måned.getLocalDateInterval());

        final var satsperioder = overlappendeSatsperioder.toSegments().stream()
            .map(it -> mapTilSatsperiode(it.getLocalDateInterval(), it.getValue()))
            .toList();
        return satsperioder;
    }

    private static BigDecimal finnUtbetaltBeløp(LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelseForMåned) {
        return tilkjentYtelseForMåned.toSegments().stream().map(LocalDateSegment::getValue)
            .map(TilkjentYtelseVerdi::redusertBeløp)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private static Optional<BigDecimal> finnRapportertInntekt(LocalDateTimeline<BigDecimal> kontrollertInntektForMåned) {
        return kontrollertInntektForMåned.toSegments().stream().map(LocalDateSegment::getValue)
            .reduce(BigDecimal::add);
    }

    private static BigDecimal finnReduksjon(LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelseForMåned) {
        return tilkjentYtelseForMåned.toSegments().stream().map(LocalDateSegment::getValue)
            .map(TilkjentYtelseVerdi::reduksjon)
            .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private static UngdomsytelseSatsPeriodeDto mapTilSatsperiode(LocalDateInterval periode, UngdomsytelseSatsPeriode p) {
        return new UngdomsytelseSatsPeriodeDto(
            periode.getFomDato(),
            periode.getTomDato(),
            p.getDagsats(),
            p.getGrunnbeløpFaktor(),
            p.getGrunnbeløp(),
            p.getSatsType(),
            p.getAntallBarn(),
            p.getDagsatsBarnetillegg(),
            Virkedager.beregnAntallVirkedager(periode.getFomDato(), periode.getTomDato()));
    }

}
