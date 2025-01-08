package no.nav.ung.sak.ytelse.beregning;

import java.math.BigDecimal;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public record TilkjentYtelsePeriode(
    DatoIntervallEntitet periode,
    Long dagsats,
    BigDecimal utbetalingsgrad
) {
}
