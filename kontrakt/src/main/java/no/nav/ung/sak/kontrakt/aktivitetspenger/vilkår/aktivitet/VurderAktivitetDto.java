package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.aktivitet;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_AKTIVITETSVILKÅR_KODE)
public class VurderAktivitetDto extends BekreftetAksjonspunktDto {

    @NotNull
    @Size(min = 0, max = 100)
    private List<@Valid VilkårAktivitetPeriodeVurderingDto> vurdertePerioder;

    public VurderAktivitetDto() {
        //for jackson
    }

    @JsonCreator
    public VurderAktivitetDto(@JsonProperty("vurdertePerioder") List<VilkårAktivitetPeriodeVurderingDto> vurdertePerioder,
                              @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurdertePerioder;
    }

    public List<VilkårAktivitetPeriodeVurderingDto> getVurdertePerioder() {
        return vurdertePerioder;
    }

}
