package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StønadsstatistikkTilkjentYtelsePeriode(
    @NotNull @Valid LocalDate fom,
    @NotNull @Valid LocalDate tom,
    @NotNull @DecimalMin("0") BigDecimal dagsats,
    /* reduksjon er reduksjon pga inntekt, for hele måneden */
    @NotNull @DecimalMin("0") BigDecimal reduksjon,
    @NotNull @Size(min = 1, max = 30) @Pattern(regexp = "^[A-ZÆØÅ_-]+$") String klassekode
) {
}
