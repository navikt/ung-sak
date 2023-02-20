package no.nav.k9.sak.kontrakt.omsorgspenger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.constraints.NotNull;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ALDERSVILKÅR_BARN)
public class AvklarAldersvilkårBarnDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "erVilkarOk", required = true)
    @NotNull
    private Boolean erVilkarOk;

    public AvklarAldersvilkårBarnDto() {
        //
    }

    public AvklarAldersvilkårBarnDto(String begrunnelse, Boolean erVilkarOk) {
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
    }

    public boolean getErVilkarOk() {
        return erVilkarOk;
    }

}
