package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsgiverOpplysningerDto {

    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "referanse")
    private final String referanse;

    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "identifikator")
    private final String identifikator;

    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "navn")
    private final String navn;

    @Valid
    @JsonProperty(value = "fødselsdato")
    private LocalDate fødselsdato;

    public ArbeidsgiverOpplysningerDto(String referanse, String identifikator, String navn, LocalDate fødselsdato) {
        this.identifikator = identifikator;
        this.referanse = referanse;
        this.navn = navn;
        this.fødselsdato = fødselsdato;
    }

    public ArbeidsgiverOpplysningerDto(String identifikator, String navn) {
        this.referanse = identifikator;
        this.identifikator = identifikator;
        this.navn = navn;
    }

    public String getReferanse() {
        return referanse;
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
}
