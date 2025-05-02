package no.nav.ung.sak.ytelse.beregning;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Dependent
public class UngdomsytelseTilkjentYtelseUtleder implements TilkjentYtelseUtleder {

    private final TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    public UngdomsytelseTilkjentYtelseUtleder(TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    @WithSpan
    @Override
    public LocalDateTimeline<DagsatsOgUtbetalingsgrad> utledTilkjentYtelseTidslinje(Long behandlingId) {
        final var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandlingId);
        return tilkjentYtelseTidslinje.mapValue(v -> new DagsatsOgUtbetalingsgrad(v.dagsats().setScale(0, RoundingMode.HALF_UP ).longValue(), BigDecimal.valueOf(v.utbetalingsgrad())));
    }

}
