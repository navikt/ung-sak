package no.nav.k9.sak.kontrakt;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class KonfigDto {

    @JsonProperty(value = "property", required = true)
    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{L}\\p{N}\\p{P}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String property;

    @JsonProperty(value = "verdi", required = true)
    @NotNull
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}\\p{M}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String verdi;

    @JsonCreator
    public KonfigDto(@JsonProperty("property") String property, @JsonProperty("verdi") String verdi) {
        this.property = property;
        this.verdi = verdi;
    }

    public String getProperty() {
        return property;
    }

    public String getVerdi() {
        return verdi;
    }
}