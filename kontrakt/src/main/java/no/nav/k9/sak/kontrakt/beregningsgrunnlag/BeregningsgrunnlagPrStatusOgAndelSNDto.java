package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsgrunnlagPrStatusOgAndelSNDto extends BeregningsgrunnlagPrStatusOgAndelDto {

    @JsonProperty(value = "pgiSnitt")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal pgiSnitt;

    @JsonProperty(value = "pgiVerdier")
    @Valid
    @Size(max = 200)
    private List<PgiDto> pgiVerdier;

    @JsonProperty(value = "næringer")
    @Valid
    @Size(max = 200)
    private List<EgenNæringDto> næringer;

    public BeregningsgrunnlagPrStatusOgAndelSNDto() {
        // trengs for deserialisering av JSON
    }

    public BigDecimal getPgiSnitt() {
        return pgiSnitt;
    }

    public void setPgiSnitt(BigDecimal pgiSnitt) {
        this.pgiSnitt = pgiSnitt;
    }

    public List<PgiDto> getPgiVerdier() {
        return pgiVerdier;
    }

    public void setPgiVerdier(List<PgiDto> pgiVerdier) {
        this.pgiVerdier = pgiVerdier;
    }

    public List<EgenNæringDto> getNæringer() {
        return næringer;
    }

    public void setNæringer(List<EgenNæringDto> næringer) {
        this.næringer = næringer;
    }
}
