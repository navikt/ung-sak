package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_BISTANDSVILKÅR_OPPHØR_KODE)
public class VurderingAvBistandsvilkårEtterAvklaringDto extends BekreftetAksjonspunktDto {

    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertePerioder;

    public VurderingAvBistandsvilkårEtterAvklaringDto() {
        // for Jackson
    }

    @JsonCreator
    public VurderingAvBistandsvilkårEtterAvklaringDto(@JsonProperty("vurdertePerioder") List<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertePerioder,
                                                      @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurdertePerioder;
    }

    public List<VurderingAvVilkårPeriodeEtterAvklaringDto> getVurdertePerioder() {
        return vurdertePerioder;
    }
}
