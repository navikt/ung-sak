package no.nav.k9.sak.kontrakt.historikk;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkinnslagEndretFeltDto {

    @JsonProperty(value = "endretFeltNavn")
    @Valid
    @NotNull
    private HistorikkEndretFeltType endretFeltNavn;

    @JsonProperty(value = "fraVerdi")
    @Valid 
    private Object fraVerdi;

    @JsonProperty(value = "klFraVerdi")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String klFraVerdi;

    @JsonProperty(value = "klNavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String klNavn;

    @JsonProperty(value = "klTilVerdi")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String klTilVerdi;

    @JsonProperty(value = "navnVerdi")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navnVerdi;

    @JsonProperty(value = "tilVerdi")
    @Valid
    private Object tilVerdi;

    public HistorikkinnslagEndretFeltDto() {
    }

    public HistorikkEndretFeltType getEndretFeltNavn() {
        return endretFeltNavn;
    }

    public Object getFraVerdi() {
        return fraVerdi;
    }

    public String getKlFraVerdi() {
        return klFraVerdi;
    }

    public String getKlNavn() {
        return klNavn;
    }

    public String getKlTilVerdi() {
        return klTilVerdi;
    }

    public String getNavnVerdi() {
        return navnVerdi;
    }

    public Object getTilVerdi() {
        return tilVerdi;
    }

    public void setEndretFeltNavn(HistorikkEndretFeltType endretFeltNavn) {
        this.endretFeltNavn = endretFeltNavn;
    }

    public void setFraVerdi(Object fraVerdi) {
        this.fraVerdi = fraVerdi;
    }

    public void setKlFraVerdi(String klFraVerdi) {
        this.klFraVerdi = klFraVerdi;
    }

    public void setKlNavn(String klNavn) {
        this.klNavn = klNavn;
    }

    public void setKlTilVerdi(String klTilVerdi) {
        this.klTilVerdi = klTilVerdi;
    }

    public void setNavnVerdi(String navnVerdi) {
        this.navnVerdi = navnVerdi;
    }

    public void setTilVerdi(Object tilVerdi) {
        this.tilVerdi = tilVerdi;
    }

}
