package no.nav.ung.sak.kontrakt.ungdomsytelse.ytelse;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.ungdomsytelse.beregning.UngdomsytelseSatsPeriodeDto;

import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;

public record UngdomsytelseUtbetaltMånedDto(
    @NotNull YearMonth måned,
    @NotNull List<UngdomsytelseSatsPeriodeDto> satsperioder,
    @NotNull int antallDager,
    BigDecimal rapportertInntekt,
    BigDecimal reduksjon,
    BigDecimal utbetaling,
    UtbetalingStatus status
) {
}
