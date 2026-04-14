package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_BISTANDSVILKÅR_KODE)
public class VurderBehovForBistandDto extends BekreftetAksjonspunktDto {

    @JsonProperty("vurdertePerioder")
    //TODO sett not null når frontend er oppdatert
    //@NotNull
    @Size(min = 0, max = 100)
    private List<@Valid VilkårPeriodeVurderingDto> vurdertePerioder;

    public VurderBehovForBistandDto() {
        //for jackson
    }

    @JsonCreator
    public VurderBehovForBistandDto(@JsonProperty("vurdertePerioder") List<VilkårPeriodeVurderingDto> vurdertePerioder,
                                    @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurdertePerioder;
    }

    public List<VilkårPeriodeVurderingDto> getVurdertePerioder() {
        return vurdertePerioder;
    }

}
