package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StønadsstatistikkSatsPeriode(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    @NotNull StønadstatistikkSatsType satsType,
    @NotNull @Min(0) int antallBarn,
    @NotNull @Min(0) int dagsatsBarnetillegg,
    @NotNull @DecimalMin("0") BigDecimal grunnbeløpFaktor
) {
}
