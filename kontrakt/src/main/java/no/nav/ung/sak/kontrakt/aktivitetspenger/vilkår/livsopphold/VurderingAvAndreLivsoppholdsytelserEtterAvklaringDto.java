package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER_OPPHØR_KODE)
public class VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto extends BekreftetAksjonspunktDto {

    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertePerioder;

    public VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto() {
        // for Jackson
    }

    @JsonCreator
    public VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto(@JsonProperty("vurdertePerioder") List<VurderingAvVilkårPeriodeEtterAvklaringDto> vurdertePerioder,
                                                                @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurdertePerioder;
    }

    public List<VurderingAvVilkårPeriodeEtterAvklaringDto> getVurdertePerioder() {
        return vurdertePerioder;
    }
}
