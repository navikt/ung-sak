package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bosted;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderingAvVilkårPeriodeEtterAvklaringDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_BOSTEDSVILKÅR_OPPHØR_KODE)
public class VurderingAvBostedsvilkårEtterAvklaringDto extends BekreftetAksjonspunktDto {

    @JsonProperty("vurdertePerioder")
    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertePerioder;

    public VurderingAvBostedsvilkårEtterAvklaringDto() {
        // for Jackson
    }

    @JsonCreator
    public VurderingAvBostedsvilkårEtterAvklaringDto(@JsonProperty("vurdertePerioder") List<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertePerioder,
                                       @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurdertePerioder;
    }

    public List<VurderingAvVilkårPeriodeEtterAvklaringDto> getVurdertePerioder() {
        return vurdertePerioder;
    }
}
