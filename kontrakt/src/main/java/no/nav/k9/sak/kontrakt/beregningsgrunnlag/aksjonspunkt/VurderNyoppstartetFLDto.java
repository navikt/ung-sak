package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderNyoppstartetFLDto {

    @JsonProperty(value = "erNyoppstartetFL", required = true)
    @NotNull
    private Boolean erNyoppstartetFL;

    public VurderNyoppstartetFLDto(Boolean erNyoppstartetFL) {
        this.erNyoppstartetFL = erNyoppstartetFL;
    }

    protected VurderNyoppstartetFLDto() {
        //
    }

    public Boolean erErNyoppstartetFL() {
        return erNyoppstartetFL;
    }

    public void setErNyoppstartetFL(Boolean erNyoppstartetFL) {
        this.erNyoppstartetFL = erNyoppstartetFL;
    }
}
