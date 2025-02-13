package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import java.math.BigDecimal;

public record TilkjentYtelseVerdi(BigDecimal uredusertBeløp, BigDecimal reduksjon, BigDecimal redusertBeløp, BigDecimal dagsats, int utbetalingsgrad) {
}
