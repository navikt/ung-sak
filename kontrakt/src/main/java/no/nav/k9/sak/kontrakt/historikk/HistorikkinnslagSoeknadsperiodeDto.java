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

import no.nav.k9.kodeverk.historikk.HistorikkAvklartSoeknadsperiodeType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkinnslagSoeknadsperiodeDto {

    @JsonProperty(value = "navnVerdi")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navnVerdi;

    @JsonProperty(value = "soeknadsperiodeType")
    @Valid
    @NotNull
    private HistorikkAvklartSoeknadsperiodeType soeknadsperiodeType;

    @JsonProperty(value = "tilVerdi")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String tilVerdi;

    public HistorikkinnslagSoeknadsperiodeDto() {
    }

    public String getNavnVerdi() {
        return navnVerdi;
    }

    public HistorikkAvklartSoeknadsperiodeType getSoeknadsperiodeType() {
        return soeknadsperiodeType;
    }

    public String getTilVerdi() {
        return tilVerdi;
    }

    public void setNavnVerdi(String navnVerdi) {
        this.navnVerdi = navnVerdi;
    }

    public void setSoeknadsperiodeType(HistorikkAvklartSoeknadsperiodeType soeknadsperiodeType) {
        this.soeknadsperiodeType = soeknadsperiodeType;
    }

    public void setTilVerdi(String tilVerdi) {
        this.tilVerdi = tilVerdi;
    }

}
