package no.nav.ung.ytelse.aktivitetspenger.formidling.dto.innvilgelse.beregning;

import java.math.BigDecimal;

public record SatsgrunnlagDto(
    BigDecimal faktor,
    long minsteÅrligeYtelse,
    long minstesats
) {
}

