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
public class BeregningsgrunnlagPrStatusOgAndelATDto extends BeregningsgrunnlagPrStatusOgAndelDto {

    @JsonProperty(value = "bortfaltNaturalytelse")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal bortfaltNaturalytelse;

    public BeregningsgrunnlagPrStatusOgAndelATDto() {
        // trengs for deserialisering av JSON
    }

    public BigDecimal getBortfaltNaturalytelse() {
        return bortfaltNaturalytelse;
    }

    public void setBortfaltNaturalytelse(BigDecimal bortfaltNaturalytelse) {
        this.bortfaltNaturalytelse = bortfaltNaturalytelse;
    }
}
