package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsgiverOgArbeidsforholdOpplysningerDto {

    @Valid
    @NotNull
    @JsonUnwrapped
    private ArbeidsgiverOpplysningDto arbeidsgiverOpplysning;

    @Valid
    @Size()
    @NotNull
    @JsonProperty(value = "arbeidsforholdreferanser")
    private List<ArbeidsforholdIdDto> arbeidsforholdreferanser;


    public ArbeidsgiverOgArbeidsforholdOpplysningerDto(String identifikator, String personIdentifikator, String navn, LocalDate fødselsdato,
                                                       List<ArbeidsforholdIdDto> arbeidsforholdreferanser) {
        this.arbeidsgiverOpplysning = new ArbeidsgiverOpplysningDto(identifikator, personIdentifikator, navn, fødselsdato);
        this.arbeidsforholdreferanser = arbeidsforholdreferanser;
    }

    public ArbeidsgiverOgArbeidsforholdOpplysningerDto(String identifikator, String navn, List<ArbeidsforholdIdDto> arbeidsforholdreferanser) {
        this.arbeidsgiverOpplysning = new ArbeidsgiverOpplysningDto(identifikator, navn);
        this.arbeidsforholdreferanser = arbeidsforholdreferanser;
    }

    public String getPersonIdentifikator() {
        return arbeidsgiverOpplysning.getPersonIdentifikator();
    }

    public String getIdentifikator() {
        return arbeidsgiverOpplysning.getIdentifikator();
    }

    public String getNavn() {
        return arbeidsgiverOpplysning.getNavn();
    }

    public LocalDate getFødselsdato() {
        return arbeidsgiverOpplysning.getFødselsdato();
    }

    public List<ArbeidsforholdIdDto> getArbeidsforholdreferanser() {
        return arbeidsforholdreferanser;
    }
}
