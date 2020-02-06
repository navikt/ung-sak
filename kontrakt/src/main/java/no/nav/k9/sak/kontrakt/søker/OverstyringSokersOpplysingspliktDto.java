package no.nav.k9.sak.kontrakt.søker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST_KODE)
public class OverstyringSokersOpplysingspliktDto extends OverstyringAksjonspunktDto {

    @JsonProperty("erVilkarOk")
    private boolean erVilkarOk;

    protected OverstyringSokersOpplysingspliktDto() {
        //
    }

    public OverstyringSokersOpplysingspliktDto(boolean erVilkarOk, String begrunnelse) { // NOSONAR
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @Override
    public boolean getErVilkarOk() {
        return erVilkarOk;
    }

}
