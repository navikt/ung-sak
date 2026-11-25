package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AktivitetspengerStønadstatistikkPeriode(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    @NotNull StønadstatistikkUtfall utfall
) {
}
