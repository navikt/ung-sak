package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TilstøtendeYtelseAndelDto extends FaktaOmBeregningAndelDto {

    @JsonProperty("fordelingForrigeYtelse")
    private BigDecimal fordelingForrigeYtelse;

    @JsonProperty("refusjonskrav")
    private BigDecimal refusjonskrav;

    @JsonProperty("fastsattPrAar")
    private BigDecimal fastsattPrAar;

    public TilstøtendeYtelseAndelDto () {
        // 
    }

    public BigDecimal getFordelingForrigeYtelse() {
        return fordelingForrigeYtelse;
    }

    public void setFordelingForrigeYtelse(BigDecimal fordelingForrigeYtelse) {
        this.fordelingForrigeYtelse = fordelingForrigeYtelse;
    }

    public BigDecimal getRefusjonskrav() {
        return refusjonskrav;
    }

    public void setRefusjonskrav(BigDecimal refusjonskrav) {
        this.refusjonskrav = refusjonskrav;
    }

    public BigDecimal getFastsattPrAar() {
        return fastsattPrAar;
    }

    public void setFastsattPrAar(BigDecimal fastsattPrAar) {
        this.fastsattPrAar = fastsattPrAar;
    }

}
