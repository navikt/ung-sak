package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
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
public class InntektDto {

    @JsonProperty(value = "belop")
    @Min(0)
    @Max(10 * 1000 * 1000)
    private Integer belop;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "navn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}\\p{M}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navn;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    @JsonProperty(value = "utbetaler")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}\\p{M}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String utbetaler;

    @JsonProperty(value = "ytelse")
    private Boolean ytelse;

    public InntektDto() {
        // trengs for deserialisering av JSON
    }

    public Integer getBelop() {
        return belop;
    }

    public LocalDate getFom() {
        return fom;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getUtbetaler() {
        return utbetaler;
    }

    public Boolean getYtelse() {
        return ytelse;
    }

    public void setBelop(Integer belop) {
        this.belop = belop;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public void setUtbetaler(String utbetaler) {
        this.utbetaler = utbetaler;
    }

    public void setYtelse(Boolean ytelse) {
        this.ytelse = ytelse;
    }
}
