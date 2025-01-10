package no.nav.ung.sak.formidling.template.dto.innvilgelse;

public record SatserDto(
    java.math.BigDecimal høy,
    java.math.BigDecimal lav,
    Integer aldersgrenseLav,
    Integer aldersgrenseHøy
) {
}
