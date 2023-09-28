package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsgiverOgArbeidsforholdOversiktDto {

    @Valid
    @Size
    @JsonProperty(value = "arbeidsgivere")
    private final Map<String, ArbeidsgiverOgArbeidsforholdOpplysningerDto> arbeidsgivere;

    public ArbeidsgiverOgArbeidsforholdOversiktDto() {
        this.arbeidsgivere = new HashMap<>();
    }

    public ArbeidsgiverOgArbeidsforholdOversiktDto(Map<String, ArbeidsgiverOgArbeidsforholdOpplysningerDto> arbeidsgivere) {
        this.arbeidsgivere = arbeidsgivere;
    }

    public Map<String, ArbeidsgiverOgArbeidsforholdOpplysningerDto> getArbeidsgivere() {
        return arbeidsgivere;
    }
}
