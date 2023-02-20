package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsgiverOpplysningerDto {

    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "identifikator")
    private final String identifikator;

    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "personIdentifikator")
    private String personIdentifikator;

    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "navn")
    private final String navn;

    @Valid
    @JsonProperty(value = "fødselsdato")
    private LocalDate fødselsdato;

    @Valid
    @Size()
    @NotNull
    @JsonProperty(value = "arbeidsforholdreferanser")
    private List<ArbeidsforholdIdDto> arbeidsforholdreferanser;


    public ArbeidsgiverOpplysningerDto(String identifikator, String alternativIdentifikator, String navn, LocalDate fødselsdato,
                                       List<ArbeidsforholdIdDto> arbeidsforholdreferanser) {
        this.identifikator = identifikator;
        this.personIdentifikator = alternativIdentifikator;
        this.navn = navn;
        this.fødselsdato = fødselsdato;
        this.arbeidsforholdreferanser = arbeidsforholdreferanser;
    }

    public ArbeidsgiverOpplysningerDto(String identifikator, String navn, List<ArbeidsforholdIdDto> arbeidsforholdreferanser) {
        this.identifikator = identifikator;
        this.navn = navn;
        this.arbeidsforholdreferanser = arbeidsforholdreferanser;
    }

    public String getPersonIdentifikator() {
        return personIdentifikator;
    }

    public String getIdentifikator() {
        return identifikator;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public List<ArbeidsforholdIdDto> getArbeidsforholdreferanser() {
        return arbeidsforholdreferanser;
    }
}
