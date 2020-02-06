package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.math.BigDecimal;
import java.util.Objects;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AndelMedBeløpDto extends FaktaOmBeregningAndelDto {

    @JsonProperty(value= "fastsattBelopPrMnd", required = true)
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
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
