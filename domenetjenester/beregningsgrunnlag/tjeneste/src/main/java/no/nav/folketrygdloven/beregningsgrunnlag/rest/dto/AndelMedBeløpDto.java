package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AndelMedBeløpDto extends FaktaOmBeregningAndelDto {

    @JsonProperty("fastsattBelopPrMnd")
    private BigDecimal fastsattBelopPrMnd;

    public BigDecimal getFastsattBelopPrMnd() {
        return fastsattBelopPrMnd;
    }

    public void setFastsattBelopPrMnd(BigDecimal fastsattBelopPrMnd) {
        this.fastsattBelopPrMnd = fastsattBelopPrMnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AndelMedBeløpDto that = (AndelMedBeløpDto) o;
        return Objects.equals(fastsattBelopPrMnd, that.fastsattBelopPrMnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fastsattBelopPrMnd);
    }
}
