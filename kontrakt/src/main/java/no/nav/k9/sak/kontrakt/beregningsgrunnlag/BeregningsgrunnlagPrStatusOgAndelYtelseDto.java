package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeregningsgrunnlagPrStatusOgAndelYtelseDto extends BeregningsgrunnlagPrStatusOgAndelDto {

    @JsonProperty(value = "belopFraMeldekortPrAar")
    @DecimalMin("0.00")
    @DecimalMax("100000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal belopFraMeldekortPrAar;

    @JsonProperty(value = "belopFraMeldekortPrMnd")
    @DecimalMin("0.00")
    @DecimalMax("100000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal belopFraMeldekortPrMnd;

    @JsonProperty(value = "oppjustertGrunnlag")
    @DecimalMin("0.00")
    @DecimalMax("100000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal oppjustertGrunnlag;

    public BeregningsgrunnlagPrStatusOgAndelYtelseDto() {
        // trengs for deserialisering av JSON
    }

    public BigDecimal getBelopFraMeldekortPrAar() {
        return belopFraMeldekortPrAar;
    }

    public BigDecimal getBelopFraMeldekortPrMnd() {
        return belopFraMeldekortPrMnd;
    }

    public BigDecimal getOppjustertGrunnlag() {
        return oppjustertGrunnlag;
    }

    public void setBelopFraMeldekortPrAar(BigDecimal belopFraMeldekortPrAar) {
        this.belopFraMeldekortPrAar = belopFraMeldekortPrAar;
    }

    public void setBelopFraMeldekortPrMnd(BigDecimal belopFraMeldekortPrMnd) {
        this.belopFraMeldekortPrMnd = belopFraMeldekortPrMnd;
    }

    public void setOppjustertGrunnlag(BigDecimal oppjustertGrunnlag) {
        this.oppjustertGrunnlag = oppjustertGrunnlag;
    }
}
