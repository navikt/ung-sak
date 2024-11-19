package no.nav.ung.sak.kontrakt.historikk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkinnslagHendelseDto {

    @JsonProperty(value = "navn", required = true)
    @NotNull
    @Valid
    private HistorikkinnslagType navn;

    @JsonProperty(value = "verdi")
    @NotNull
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String verdi;

    public HistorikkinnslagHendelseDto() {
        //
    }

    public HistorikkinnslagHendelseDto(HistorikkinnslagType navn, String verdi) {
        this.navn = navn;
        this.verdi = verdi;
    }

    public HistorikkinnslagType getNavn() {
        return navn;
    }

    public String getVerdi() {
        return verdi;
    }

    public void setNavn(HistorikkinnslagType navn) {
        this.navn = navn;
    }

    public void setVerdi(String verdi) {
        this.verdi = verdi;
    }

}
