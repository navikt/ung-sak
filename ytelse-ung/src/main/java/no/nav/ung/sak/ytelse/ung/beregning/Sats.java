package no.nav.ung.sak.ytelse.ung.beregning;

import java.math.BigDecimal;
import java.math.RoundingMode;

import no.nav.k9.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

public enum Sats {

    LAV(BigDecimal.valueOf(4).divide(BigDecimal.valueOf(3), 5, RoundingMode.HALF_UP), UngdomsytelseSatsType.LAV),
    HØY(BigDecimal.valueOf(2), UngdomsytelseSatsType.HØY);

    private BigDecimal grunnbeløpFaktor;
    private UngdomsytelseSatsType satsType;

    Sats(BigDecimal grunnbeløpFaktor, UngdomsytelseSatsType satsType) {
        this.grunnbeløpFaktor = grunnbeløpFaktor;
        this.satsType = satsType;
    }

    public BigDecimal getGrunnbeløpFaktor() {
        return grunnbeløpFaktor;
    }

    public UngdomsytelseSatsType getSatsType() {
        return satsType;
    }
}
