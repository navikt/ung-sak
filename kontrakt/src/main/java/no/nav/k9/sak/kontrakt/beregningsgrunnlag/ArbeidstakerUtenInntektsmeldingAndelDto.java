package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class ArbeidstakerUtenInntektsmeldingAndelDto extends FaktaOmBeregningAndelDto {
    
    @JsonProperty(value = "mottarYtelse")
    private Boolean mottarYtelse;

    @JsonProperty(value = "inntektPrMnd")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal inntektPrMnd;

    public Boolean getMottarYtelse() {
        return mottarYtelse;
    }

    public void setMottarYtelse(boolean mottarYtelse) {
        this.mottarYtelse = mottarYtelse;
    }

    public BigDecimal getInntektPrMnd() {
        return inntektPrMnd;
    }

    public void setInntektPrMnd(BigDecimal inntektPrMnd) {
        this.inntektPrMnd = inntektPrMnd;
    }
}
