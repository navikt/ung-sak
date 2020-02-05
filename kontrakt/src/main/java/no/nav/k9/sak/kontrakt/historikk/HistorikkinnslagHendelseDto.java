package no.nav.k9.sak.kontrakt.historikk;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;

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
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
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
