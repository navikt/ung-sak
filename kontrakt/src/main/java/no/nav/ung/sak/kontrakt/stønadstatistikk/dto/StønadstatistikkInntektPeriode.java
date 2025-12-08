package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StønadstatistikkInntektPeriode(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    @NotNull BigDecimal inntekt
) {
}

