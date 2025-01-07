package no.nav.ung.sak.ytelse.beregning;

import java.util.List;
import java.util.Optional;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;

public interface UtledTilkjentYtelse {

    Optional<List<TilkjentYtelsePeriode>> utledTilkjentYtelsePerioder(Long behandlingId);

    LocalDateTimeline<DagsatsOgUtbetalingsgrad> utledTilkjentYtelseTidslinje(Long behandlingId);



}
