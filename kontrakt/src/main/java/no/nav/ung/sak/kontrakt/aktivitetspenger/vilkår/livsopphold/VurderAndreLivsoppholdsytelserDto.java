package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER_KODE)
public class VurderAndreLivsoppholdsytelserDto extends BekreftetAksjonspunktDto {

    @JsonProperty("vurdertePerioder")
    @NotNull
    @Size(min = 0, max = 100)
    private List<@Valid VilkårLivsoppholdsytelserPeriodeVurderingDto> vurdertePerioder;

    public VurderAndreLivsoppholdsytelserDto() {
        //for jackson
    }

    @JsonCreator
    public VurderAndreLivsoppholdsytelserDto(
        @JsonProperty("vurdertePerioder") List<VilkårLivsoppholdsytelserPeriodeVurderingDto> vurdertePerioder,
        @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurdertePerioder;
    }

    public List<VilkårLivsoppholdsytelserPeriodeVurderingDto> getVurdertePerioder() {
        return vurdertePerioder;
    }

}
