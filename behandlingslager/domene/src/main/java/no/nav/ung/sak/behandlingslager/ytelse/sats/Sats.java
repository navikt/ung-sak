package no.nav.ung.sak.behandlingslager.ytelse.sats;

import java.math.BigDecimal;
import java.math.RoundingMode;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

public enum Sats {

    LAV(BigDecimal.valueOf(4).divide(BigDecimal.valueOf(3), 5, RoundingMode.HALF_UP), UngdomsytelseSatsType.LAV, 18, 25),
    HØY(BigDecimal.valueOf(2), UngdomsytelseSatsType.HØY, 25, 29);

    private final BigDecimal grunnbeløpFaktor;
    private final UngdomsytelseSatsType satsType;
    private final int fomAlder;
    private final int tomAlder;

    Sats(BigDecimal grunnbeløpFaktor, UngdomsytelseSatsType satsType, int fomAlder, int tomAlder) {
        this.grunnbeløpFaktor = grunnbeløpFaktor;
        this.satsType = satsType;
        this.fomAlder = fomAlder;
        this.tomAlder = tomAlder;
    }

    public BigDecimal getGrunnbeløpFaktor() {
        return grunnbeløpFaktor;
    }

    public UngdomsytelseSatsType getSatsType() {
        return satsType;
    }

    public int getFomAlder() {
        return fomAlder;
    }

    public int getTomAlder() {
        return tomAlder;
    }
}
