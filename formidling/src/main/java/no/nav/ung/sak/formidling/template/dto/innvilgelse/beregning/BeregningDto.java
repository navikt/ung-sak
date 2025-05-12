package no.nav.ung.sak.formidling.template.dto.innvilgelse.beregning;

import java.math.BigDecimal;

public record BeregningDto(
    BigDecimal faktor,
    long årsbeløp,
    long dagsats
) {
}
