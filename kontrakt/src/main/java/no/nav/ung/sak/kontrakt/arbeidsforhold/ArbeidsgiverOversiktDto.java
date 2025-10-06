package no.nav.ung.sak.kontrakt.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsgiverOversiktDto {

    @Valid
    @Size
    @JsonProperty(value = "arbeidsgivere")
    private final Map<String, ArbeidsgiverOpplysningerDto> arbeidsgivere;

    public ArbeidsgiverOversiktDto() {
        this.arbeidsgivere = new HashMap<>();
    }

    public ArbeidsgiverOversiktDto(Map<String, ArbeidsgiverOpplysningerDto> arbeidsgivere) {
        this.arbeidsgivere = arbeidsgivere;
    }

    public Map<String, ArbeidsgiverOpplysningerDto> getArbeidsgivere() {
        return arbeidsgivere;
    }
}
