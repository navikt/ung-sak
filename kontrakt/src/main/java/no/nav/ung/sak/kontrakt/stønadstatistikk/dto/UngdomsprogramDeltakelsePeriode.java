package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UngdomsprogramDeltakelsePeriode(
    @NotNull LocalDate programdeltakelseFom,
    LocalDate programdeltakelseTom
) {
}

