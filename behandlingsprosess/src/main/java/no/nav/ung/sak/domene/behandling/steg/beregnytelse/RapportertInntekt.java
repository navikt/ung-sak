package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.ung.sak.behandlingslager.behandling.sporing.Sporingsverdi;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.InntektType;

import java.math.BigDecimal;
import java.util.Objects;

public final class RapportertInntekt extends Sporingsverdi {
    private final InntektType inntektType;
    private final BigDecimal beløp;

    public RapportertInntekt(
        InntektType inntektType,
        BigDecimal beløp) {
        this.inntektType = inntektType;
        this.beløp = beløp;
    }

    @Override
    public String toString() {
        return "RapportertInntekt{" +
            "inntektType=" + inntektType +
            ", beløp=" + beløp +
            '}';
    }

    @Override
    public String tilRegelVerdi() {
        return toString();
    }

    public InntektType inntektType() {
        return inntektType;
    }

    public BigDecimal beløp() {
        return beløp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RapportertInntekt) obj;
        return Objects.equals(this.inntektType, that.inntektType) &&
            Objects.equals(this.beløp, that.beløp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inntektType, beløp);
    }

}
