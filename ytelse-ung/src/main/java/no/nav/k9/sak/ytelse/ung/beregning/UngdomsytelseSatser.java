package no.nav.k9.sak.ytelse.ung.beregning;

import java.math.BigDecimal;

import no.nav.k9.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

public record UngdomsytelseSatser(BigDecimal dagsats, BigDecimal grunnbeløp, BigDecimal grunnbeløpFaktor, UngdomsytelseSatsType satsType) {

    @Override
    public String toString() {
        return "UngdomsytelseSatser{" +
            "dagsats=" + dagsats +
            ", grunnbeløp=" + grunnbeløp +
            ", grunnbeløpFaktor=" + grunnbeløpFaktor +
            ", satsType=" + satsType +
            '}';
    }
}
