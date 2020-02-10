package no.nav.k9.sak.kontrakt;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeltFeilDto implements Serializable {

    @JsonProperty(value = "navn", required = true)
    @NotNull
    @Size(max = 2000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navn;

    @JsonProperty(value = "melding", required = true)
    @NotNull
    @Size(max = 2000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")

    private String melding;

    @JsonProperty(value = "metainformasjon")
    @Size(max = 20000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String metainformasjon;

    protected FeltFeilDto() {
        //
    }

    public FeltFeilDto(String navn, String melding) {
        this.navn = navn;
        this.melding = melding;
    }

    public FeltFeilDto(String navn, String melding, String metainformasjon) {
        this.navn = navn;
        this.melding = melding;
        this.metainformasjon = metainformasjon;
    }

    public String getNavn() {
        return navn;
    }

    public String getMelding() {
        return melding;
    }

    public String getMetainformasjon() {
        return metainformasjon;
    }
}
