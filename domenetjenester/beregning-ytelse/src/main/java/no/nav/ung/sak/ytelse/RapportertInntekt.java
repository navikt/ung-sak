package no.nav.ung.sak.ytelse;

import java.math.BigDecimal;
import java.util.Objects;

public class RapportertInntekt {
    private InntektType inntektType;
    private BigDecimal beløp;

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
