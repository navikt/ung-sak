package no.nav.ung.sak.kontrakt.ekstern.samordning;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param dagsats             utbetalt dagsats, inkludert barnetillegg og etter reduksjon for inntekt. Tom når perioden ikke er
 *                            beregnet ennå, f.eks. framtidige måneder.
 * @param beløp               dagsats ganget med antall virkedager i perioden. Tom når perioden ikke er beregnet ennå.
 * @param antallBarn          antall barn det gis barnetillegg for
 * @param dagsatsBarnetillegg barnetillegg per dag før eventuell reduksjon for inntekt
 */
public record SamordningPeriode(
    @NotNull LocalDate fom,
    @NotNull LocalDate tom,
    BigDecimal dagsats,
    BigDecimal beløp,
    Integer antallBarn,
    Integer dagsatsBarnetillegg
) {
}
