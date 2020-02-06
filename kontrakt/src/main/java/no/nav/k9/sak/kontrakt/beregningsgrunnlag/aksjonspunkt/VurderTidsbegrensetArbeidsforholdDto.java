package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderTidsbegrensetArbeidsforholdDto {

    @JsonProperty(value = "fastsatteArbeidsforhold", required = true)
    @Valid
    @NotNull
    @Size(max = 100)
    private List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold;

    protected VurderTidsbegrensetArbeidsforholdDto() {
        //
    }

    public VurderTidsbegrensetArbeidsforholdDto(List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold) { // NOSONAR
        this.fastsatteArbeidsforhold = new ArrayList<>(fastsatteArbeidsforhold);
    }

    public List<VurderteArbeidsforholdDto> getFastsatteArbeidsforhold() {
        return fastsatteArbeidsforhold;
    }

    public void setFastsatteArbeidsforhold(List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold) {
        this.fastsatteArbeidsforhold = fastsatteArbeidsforhold;
    }
}
