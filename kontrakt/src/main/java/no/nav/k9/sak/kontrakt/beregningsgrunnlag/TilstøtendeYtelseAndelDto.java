package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TilstøtendeYtelseAndelDto extends FaktaOmBeregningAndelDto {

    @JsonProperty("fastsattPrAar")
    @DecimalMin("0.00")
    @DecimalMax("1000000.00")
    @Digits(integer = 7, fraction = 2)
    private BigDecimal fastsattPrAar;

    @JsonProperty("fordelingForrigeYtelse")
    @DecimalMin("0.00")
    @DecimalMax("1000.00")
    @Digits(integer = 4, fraction = 2)
    private BigDecimal fordelingForrigeYtelse;

    @JsonProperty("refusjonskrav")
    @DecimalMin("0.00")
    @DecimalMax("1000000.00")
    @Digits(integer = 7, fraction = 2)
    private BigDecimal refusjonskrav;

    public TilstøtendeYtelseAndelDto () {
        // 
    }

    public BigDecimal getFastsattPrAar() {
        return fastsattPrAar;
    }

    public BigDecimal getFordelingForrigeYtelse() {
        return fordelingForrigeYtelse;
    }

    public BigDecimal getRefusjonskrav() {
        return refusjonskrav;
    }

    public void setFastsattPrAar(BigDecimal fastsattPrAar) {
        this.fastsattPrAar = fastsattPrAar;
    }

    public void setFordelingForrigeYtelse(BigDecimal fordelingForrigeYtelse) {
        this.fordelingForrigeYtelse = fordelingForrigeYtelse;
    }

    public void setRefusjonskrav(BigDecimal refusjonskrav) {
        this.refusjonskrav = refusjonskrav;
    }

}
