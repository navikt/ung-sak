package no.nav.ung.sak.kontroll;

import java.math.BigDecimal;
import java.util.Objects;

public class RapportertInntekt {
    private InntektType inntektType;
    private BigDecimal beløp;
    private boolean negativRegisterinntekt;

    public RapportertInntekt(
        InntektType inntektType,
        BigDecimal beløp) {
        this(inntektType, beløp, false);
    }

    private RapportertInntekt(InntektType inntektType, BigDecimal beløp, boolean negativRegisterinntekt) {
        this.inntektType = inntektType;
        this.beløp = beløp;
        this.negativRegisterinntekt = negativRegisterinntekt;
    }

    /**
     * Registerinntekt der summen for perioden var negativ, f.eks. fordi arbeidsgiver har korrigert tidligere rapportert inntekt.
     * Beløpet tolkes som 0 kr, men skal alltid vurderes av saksbehandler.
     */
    public static RapportertInntekt negativRegisterinntektTolketSomNull(InntektType inntektType) {
        return new RapportertInntekt(inntektType, BigDecimal.ZERO, true);
    }

    @Override
    public String toString() {
        return "RapportertInntekt{" +
            "inntektType=" + inntektType +
            ", beløp=" + beløp +
            ", negativRegisterinntekt=" + negativRegisterinntekt +
            '}';
    }

    public InntektType inntektType() {
        return inntektType;
    }

    public BigDecimal beløp() {
        return beløp;
    }

    public boolean erNegativRegisterinntekt() {
        return negativRegisterinntekt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RapportertInntekt) obj;
        return Objects.equals(this.inntektType, that.inntektType) &&
            Objects.equals(this.beløp, that.beløp) &&
            this.negativRegisterinntekt == that.negativRegisterinntekt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inntektType, beløp, negativRegisterinntekt);
    }

}
