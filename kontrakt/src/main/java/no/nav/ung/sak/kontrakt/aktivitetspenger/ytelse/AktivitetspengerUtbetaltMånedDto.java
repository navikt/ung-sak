package no.nav.ung.sak.kontrakt.aktivitetspenger.ytelse;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.AktivitetspengerSatsPeriodeDto;
import no.nav.ung.sak.kontrakt.ungdomsytelse.ytelse.UtbetalingStatus;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record AktivitetspengerUtbetaltMånedDto(
    @NotNull boolean gjelderDelerAvMåned,
    @NotNull YearMonth måned,
    @NotNull List<AktivitetspengerSatsPeriodeDto> satsperioder,
    @NotNull int antallDager,
    BigDecimal rapportertInntekt,
    BigDecimal reduksjonsgrunnlag,
    BigDecimal reduksjon,
    BigDecimal utbetaling,
    UtbetalingStatus status
) {
}

