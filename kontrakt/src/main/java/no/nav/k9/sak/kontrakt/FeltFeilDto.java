package no.nav.k9.sak.kontrakt;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeltFeilDto implements Serializable {

    @JsonProperty(value = "melding", required = true)
    @NotNull
    @Size(max = 2000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String melding;

    @JsonProperty(value = "metainformasjon")
    @Size(max = 20000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String metainformasjon;

    @JsonProperty(value = "navn", required = true)
    @NotNull
    @Size(max = 2000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    public FeltFeilDto(String navn, String melding) {
        this.navn = navn;
        this.melding = melding;
    }

    public FeltFeilDto(String navn, String melding, String metainformasjon) {
        this.navn = navn;
        this.melding = melding;
        this.metainformasjon = metainformasjon;
    }

    protected FeltFeilDto() {
        //
    }

    public String getMelding() {
        return melding;
    }

    public String getMetainformasjon() {
        return metainformasjon;
    }

    public String getNavn() {
        return navn;
    }
}
