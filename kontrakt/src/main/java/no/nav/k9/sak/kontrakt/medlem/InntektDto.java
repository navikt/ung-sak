package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;

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
    
    @JsonProperty(value="navn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}\\p{M}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navn;
    
    @JsonProperty(value="utbetaler")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}\\p{M}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String utbetaler;
    
    @JsonProperty(value="fom", required = true)
    @NotNull
    private LocalDate fom;
    
    @JsonProperty(value="tom", required = true)
    @NotNull
    private LocalDate tom;
    
    @JsonProperty(value="ytelse")
    private Boolean ytelse;
    
    @JsonProperty(value="belop")
    @Min(0)
    private Integer belop;

    public InntektDto() {
        // trengs for deserialisering av JSON
    }
    public String getNavn() {
        return navn;
    }

    public String getUtbetaler() {
        return utbetaler;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public Boolean getYtelse() {
        return ytelse;
    }

    public Integer getBelop() {
        return belop;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setUtbetaler(String utbetaler) {
        this.utbetaler = utbetaler;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public void setYtelse(Boolean ytelse) {
        this.ytelse = ytelse;
    }

    public void setBelop(Integer belop) {
        this.belop = belop;
    }
}
