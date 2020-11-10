package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
