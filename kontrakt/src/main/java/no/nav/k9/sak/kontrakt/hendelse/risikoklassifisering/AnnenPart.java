package no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AnnenPart {

    @JsonProperty(value = "annenPartAktoerId")
    private AktoerId annenPartAktoerId;

    @JsonProperty(value = "utenlandskFnr")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String utenlandskFnr;

    public AnnenPart(AktoerId annenPartAktoerId) {
        this.annenPartAktoerId = annenPartAktoerId;
        this.utenlandskFnr = null;
    }

    public AnnenPart(String utenlandskFnr) {
        this.annenPartAktoerId = null;
        this.utenlandskFnr = utenlandskFnr;
    }

    public AktoerId getAnnenPartAktoerId() {
        return annenPartAktoerId;
    }

    public String getUtenlandskFnr() {
        return utenlandskFnr;
    }
}
