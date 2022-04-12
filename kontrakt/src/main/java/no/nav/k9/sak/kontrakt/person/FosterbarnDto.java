package no.nav.k9.sak.kontrakt.person;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class FosterbarnDto {

    @JsonProperty(value = "fnr")
    @Size(max = 11)
    @Pattern(regexp = "^[\\p{Alnum}]{11}+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String fnr;

    @JsonProperty(value = "navn", required = true)
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    @JsonProperty(value = "fødselsdato", required = true)
    @Valid
    private LocalDate fødselsdato;

    public FosterbarnDto(String fnr, String navn, LocalDate fødselsdato) {
        this.fnr = fnr;
        this.navn = navn;
        this.fødselsdato = fødselsdato;
    }
}
