package no.nav.ung.sak.formidling.template.dto.innvilgelse;

import java.math.BigDecimal;

public record TilkjentYtelseDto(
    long dagsats,
    BigDecimal satsFaktor,
    long gBeløp,
    long årsbeløp
) {
}
