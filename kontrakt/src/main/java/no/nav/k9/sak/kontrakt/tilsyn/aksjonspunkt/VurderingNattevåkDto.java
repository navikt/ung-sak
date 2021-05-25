package no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt;

import com.fasterxml.jackson.annotation.*;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_NATTEVÅK)
public class VurderingNattevåkDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "vurderinger")
    @Size(max = 1000)
    @Valid
    private List<VurderingDto> vurderinger;

    public VurderingNattevåkDto() {
        //
    }

    public VurderingNattevåkDto(List<VurderingDto> vurderinger) {
        this.vurderinger = vurderinger;
    }

    public List<VurderingDto> getVurderinger() {
        return vurderinger;
    }

}
