package no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AktivitetspengerPeriode(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom
) {
}
