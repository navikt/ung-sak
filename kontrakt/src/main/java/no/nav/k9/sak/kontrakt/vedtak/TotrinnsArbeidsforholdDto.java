package no.nav.k9.sak.kontrakt.vedtak;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TotrinnsArbeidsforholdDto {

    @JsonProperty(value = "navn", required = true)
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navn;

    /**
     * FIXME K9: dårlig design - felt kan inneholde fødselsdato (for privat arbeisgiver) eller orgnummer (for virksomhet). Beholder
     * organisasjonsnummer som serialisert property for bakoverkompatibliitet.
     */
    @JsonAlias({ "arbeidsgiverIdentifikator" })
    @JsonProperty(value = "organisasjonsnummer")
    @Size(max = 20)
    @Pattern(regexp = "^[1-9][0-9\\-.]{6,20}+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverIdentifikator;

    @JsonProperty(value = "arbeidsforholdId")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdId;

    @JsonProperty(value = "arbeidsforholdHandlingType")
    @Valid
    private ArbeidsforholdHandlingType arbeidsforholdHandlingType;

    @JsonProperty(value = "brukPermisjon")
    private Boolean brukPermisjon;

    public TotrinnsArbeidsforholdDto(String navn,
                                     String arbeidsgiverIdentifikator,
                                     String arbeidsforholdId,
                                     ArbeidsforholdHandlingType handling,
                                     Boolean brukPermisjon) {
        this.navn = navn;
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
        this.arbeidsforholdId = arbeidsforholdId;
        this.arbeidsforholdHandlingType = handling;
        this.brukPermisjon = brukPermisjon;
    }

    public String getNavn() {
        return navn;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public ArbeidsforholdHandlingType getArbeidsforholdHandlingType() {
        return arbeidsforholdHandlingType;
    }

    public Boolean getBrukPermisjon() {
        return brukPermisjon;
    }

}
