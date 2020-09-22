package no.nav.k9.sak.kontrakt.arbeidsforhold;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ArbeidsgiverDto {

    // For mottak fra GUI (orgnr for virksomhet, og aktørId for person-arbeidsgiver)
    @JsonProperty(value = "identifikator")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String identifikator;

    // For visning i GUI (orgnr for virksomhet, og fødselsdato formatert dd.MM.yyyy for person-arbeidsgiver)
    @JsonProperty(value = "identifikatorGUI")
    @JsonAlias(value = "arbeidsgiverIdentifikatorForVisning")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String identifikatorGUI;

    @JsonProperty(value = "navn")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    @JsonCreator
    public ArbeidsgiverDto(@Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String identifikator,
                           @Size(max = 100) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String identifikatorGUI,
                           String navn) {
        this.identifikator = identifikator;
        this.identifikatorGUI = identifikatorGUI;
        this.navn = navn;
    }

    public String getIdentifikator() {
        return identifikator;
    }

    public String getIdentifikatorGUI() {
        return identifikatorGUI;
    }

    public String getNavn() {
        return navn;
    }

    @Override
    public String toString() {
        return "ArbeidsgiverDto{" +
            "arbeidsgiverIdentifikator='" + identifikator + '\'' +
            ", arbeidsgiverIdentifikatorGUI='" + identifikatorGUI + '\'' +
            '}';
    }
}
