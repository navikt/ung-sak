package no.nav.ung.sak.kontrakt.historikk;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkInnslagGjeldendeFraDto {

    @JsonProperty(value = "fra")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String fra;

    @JsonProperty(value = "navn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    @JsonProperty(value = "verdi")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String verdi;

    public HistorikkInnslagGjeldendeFraDto(String fra) {
        this.fra = fra;
    }

    public HistorikkInnslagGjeldendeFraDto(String fra, String navn, String verdi) {
        this.fra = fra;
        this.navn = navn;
        this.verdi = verdi;
    }

    HistorikkInnslagGjeldendeFraDto() {
        //
    }

    public String getFra() {
        return fra;
    }

    public String getNavn() {
        return navn;
    }

    public String getVerdi() {
        return verdi;
    }

    public void setFra(String fra) {
        this.fra = fra;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setVerdi(String verdi) {
        this.verdi = verdi;
    }
}
