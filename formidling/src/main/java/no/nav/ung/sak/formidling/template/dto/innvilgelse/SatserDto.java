package no.nav.ung.sak.formidling.template.dto.innvilgelse;

import java.math.BigDecimal;

public record SatserDto(
    BigDecimal høy,
    BigDecimal lav,
    Integer aldersgrenseLav,
    Integer aldersgrenseHøy
) {
}
