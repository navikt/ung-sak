package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BeregningsgrunnlagPrStatusOgAndelSNDto extends BeregningsgrunnlagPrStatusOgAndelDto {

    @JsonProperty("pgiSnitt")
    private BigDecimal pgiSnitt;

    @JsonProperty("pgiVerdier")
    private List<PgiDto> pgiVerdier;

    @JsonProperty("næringer")
    private List<EgenNæringDto> næringer;

    public BeregningsgrunnlagPrStatusOgAndelSNDto() {
        super();
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
