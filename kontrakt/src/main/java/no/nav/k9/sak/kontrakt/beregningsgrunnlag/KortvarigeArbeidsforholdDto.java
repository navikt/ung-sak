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
public class KortvarigeArbeidsforholdDto extends FaktaOmBeregningAndelDto {

    @JsonProperty(value = "erTidsbegrensetArbeidsforhold", required = true)
    @NotNull
    private Boolean erTidsbegrensetArbeidsforhold;

    public KortvarigeArbeidsforholdDto() {
        //
    }

    public Boolean getErTidsbegrensetArbeidsforhold() {
        return erTidsbegrensetArbeidsforhold;
    }

    public void setErTidsbegrensetArbeidsforhold(Boolean erTidsbegrensetArbeidsforhold) {
        this.erTidsbegrensetArbeidsforhold = erTidsbegrensetArbeidsforhold;
    }

}
