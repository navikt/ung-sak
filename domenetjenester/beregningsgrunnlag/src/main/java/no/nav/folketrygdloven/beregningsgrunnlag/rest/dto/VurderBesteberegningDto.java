package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderBesteberegningDto {

    @JsonProperty(value="skalHaBesteberegning",required = true)
    @NotNull
    private Boolean skalHaBesteberegning;

    public Boolean getSkalHaBesteberegning() {
        return skalHaBesteberegning;
    }

    public void setSkalHaBesteberegning(Boolean skalHaBesteberegning) {
        this.skalHaBesteberegning = skalHaBesteberegning;
    }
}
