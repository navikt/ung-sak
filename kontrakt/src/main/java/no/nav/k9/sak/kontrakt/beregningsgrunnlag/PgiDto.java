package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PgiDto {

    @JsonProperty(value = "beløp", required = true)
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal beløp;

    @JsonProperty(value = "årstall")
    @Min(2000)
    @Max(2100)
    private Integer årstall;

    protected PgiDto() {
        //
    }

    public PgiDto(BigDecimal beløp, Integer årstall) {
        this.beløp = beløp;
        this.årstall = årstall;
    }

    public BigDecimal getBeløp() {
        return beløp;
    }

    public Integer getÅrstall() {
        return årstall;
    }
}
