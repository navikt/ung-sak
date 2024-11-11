package no.nav.ung.sak.kontrakt.person;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE_KODE)
public class OverstyringUtenlandssakMarkeringDto extends OverstyringAksjonspunktDto {

    @JsonProperty(value = "gammelVerdi")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String gammelVerdi;

    public OverstyringUtenlandssakMarkeringDto() {
        //
    }

    public OverstyringUtenlandssakMarkeringDto(String begrunnelse, String gammelVerdi) {
        super(null, begrunnelse);
        this.gammelVerdi = gammelVerdi;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        // Brukes ikke
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        // Brukes ikke
        return false;
    }

    @JsonGetter
    public String getGammelVerdi() {
        return gammelVerdi;
    }

    public void setGammelVerdi(String gammelVerdi) {
        this.gammelVerdi = gammelVerdi;
    }

}
