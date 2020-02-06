package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsgrunnlagPrStatusOgAndelFLDto extends BeregningsgrunnlagPrStatusOgAndelDto {

    @JsonProperty(value = "erNyoppstartet", required = true)
    @NotNull
    private Boolean erNyoppstartet;

    public BeregningsgrunnlagPrStatusOgAndelFLDto() {
        super();
        // trengs for deserialisering av JSON
    }

    public Boolean getErNyoppstartet() {
        return erNyoppstartet;
    }

    public void setErNyoppstartet(Boolean erNyoppstartet) {
        this.erNyoppstartet = erNyoppstartet;
    }

}
