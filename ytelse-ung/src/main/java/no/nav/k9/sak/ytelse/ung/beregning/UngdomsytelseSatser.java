package no.nav.k9.sak.ytelse.ung.beregning;

import java.math.BigDecimal;

public record UngdomsytelseSatser(BigDecimal dagsats, BigDecimal grunnbeløp, BigDecimal grunnbeløpFaktor) {

    @Override
    public String toString() {
        return "UngdomsytelseSatser{" +
            "dagsats=" + dagsats +
            ", grunnbeløp=" + grunnbeløp +
            ", grunnbeløpFaktor=" + grunnbeløpFaktor +
            '}';
    }
}
