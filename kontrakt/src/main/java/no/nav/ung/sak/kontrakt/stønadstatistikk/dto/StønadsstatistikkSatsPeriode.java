package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StønadsstatistikkSatsPeriode(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    @NotNull StønadstatistikkSatsType satsType,
    @NotNull @DecimalMin("0") int antallBarn
) {
}
