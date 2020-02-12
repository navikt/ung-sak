package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.Collections;
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

    @JsonProperty(value = "næringer")
    @Valid
    @Size(max = 200)
    private List<EgenNæringDto> næringer = Collections.emptyList();

    @JsonProperty(value = "pgiSnitt")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal pgiSnitt;

    @JsonProperty(value = "pgiVerdier")
    @Valid
    @Size(max = 200)
    private List<PgiDto> pgiVerdier = Collections.emptyList();

    public BeregningsgrunnlagPrStatusOgAndelSNDto() {
        // trengs for deserialisering av JSON
    }

    public List<EgenNæringDto> getNæringer() {
        return næringer;
    }

    public BigDecimal getPgiSnitt() {
        return pgiSnitt;
    }

    public List<PgiDto> getPgiVerdier() {
        return Collections.unmodifiableList(pgiVerdier);
    }

    public void setNæringer(List<EgenNæringDto> næringer) {
        this.næringer = List.copyOf(næringer);
    }

    public void setPgiSnitt(BigDecimal pgiSnitt) {
        this.pgiSnitt = pgiSnitt;
    }

    public void setPgiVerdier(List<PgiDto> pgiVerdier) {
        this.pgiVerdier = List.copyOf(pgiVerdier);
    }
}
