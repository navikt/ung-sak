package no.nav.ung.sak.ytelse.beregning;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;

public interface TilkjentYtelseUtleder {

    LocalDateTimeline<DagsatsOgUtbetalingsgrad> utledTilkjentYtelseTidslinje(Long behandlingId);

}
