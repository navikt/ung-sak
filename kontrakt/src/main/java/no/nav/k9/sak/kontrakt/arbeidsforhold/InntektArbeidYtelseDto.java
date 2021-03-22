package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class InntektArbeidYtelseDto {

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "arbeidsforhold")
    private List<InntektArbeidYtelseArbeidsforhold> arbeidsforhold = Collections.emptyList();

    public InntektArbeidYtelseDto() {
        //
    }

    public void setArbeidsforhold(List<InntektArbeidYtelseArbeidsforhold> arbeidsforhold) {
        this.arbeidsforhold = List.copyOf(arbeidsforhold);
    }

}
