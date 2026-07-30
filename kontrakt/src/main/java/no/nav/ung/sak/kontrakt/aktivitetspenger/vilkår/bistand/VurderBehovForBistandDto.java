package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_BISTANDSVILKÅR_KODE)
public class VurderBehovForBistandDto extends BekreftetAksjonspunktDto {

    @JsonProperty("vurdertePerioder")
    @NotNull
    @Size(min = 0, max = 100)
    private List<@Valid VilkårBistandPeriodeVurderingDto> vurdertePerioder;

    public VurderBehovForBistandDto() {
        //for jackson
    }

    @JsonCreator
    public VurderBehovForBistandDto(@JsonProperty("vurdertePerioder") List<VilkårBistandPeriodeVurderingDto> vurdertePerioder,
                                    @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurdertePerioder;
    }

    public List<VilkårBistandPeriodeVurderingDto> getVurdertePerioder() {
        return vurdertePerioder;
    }

}
