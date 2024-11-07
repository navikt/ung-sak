package no.nav.k9.sak.kontrakt.person;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PersonopplysningPleietrengendeDto {

    @JsonProperty(value = "fnr")
    @Size(max = 11)
    @Pattern(regexp = "^[\\p{Alnum}]{11}+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String fnr;

    @JsonProperty(value = "navn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    @JsonProperty(value = "diagnosekoder")
    @Size(max = 100)
    @Valid
    private List<String> diagnosekoder;

    @JsonAlias("dødsdato")
    @JsonProperty(value = "dodsdato")
    @Valid
    private LocalDate dodsdato;


    public PersonopplysningPleietrengendeDto() {
        //
    }

    public String getFnr() {
        return fnr;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public List<String> getDiagnosekoder() {
        return diagnosekoder;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public void setDiagnosekoder(List<String> diagnosekoder) {
        this.diagnosekoder = diagnosekoder;
    }

    public LocalDate getDodsdato() {
        return dodsdato;
    }

    public void setDodsdato(LocalDate dodsdato) {
        this.dodsdato = dodsdato;
    }

}
