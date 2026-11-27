package no.nav.ung.sak.kontrakt.ekstern.ytelse;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * @param antallBarn          antall barn det gis barnetillegg for
 * @param dagsatsBarnetillegg barnetillegg per dag før eventuell reduksjon for inntekt
 */
public record BarnetilleggPeriode(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    int antallBarn,
    int dagsatsBarnetillegg
) {
}
