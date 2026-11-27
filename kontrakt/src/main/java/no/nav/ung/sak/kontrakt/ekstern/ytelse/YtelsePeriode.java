package no.nav.ung.sak.kontrakt.ekstern.ytelse;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param dagsats utbetalt dagsats, inkludert barnetillegg og etter reduksjon for inntekt. Tom når perioden ikke er
 *                beregnet ennå, f.eks. framtidige måneder.
 * @param beløp   dagsats ganget med antall virkedager i perioden. Tom når perioden ikke er beregnet ennå.
 */
public record YtelsePeriode(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    BigDecimal dagsats,
    BigDecimal beløp
) {
}
