package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OPPHØR_BOSTED_KODE)
public class VurderOpphørBostedDto extends BekreftetAksjonspunktDto {

    @JsonProperty("vurdertePerioder")
    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid OpphørPeriodeVurderingDto> vurdertePerioder;

    public VurderOpphørBostedDto() {
        // for Jackson
    }

    @JsonCreator
    public VurderOpphørBostedDto(@JsonProperty("vurdertePerioder") List<OpphørPeriodeVurderingDto> vurdertePerioder,
                                 @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurdertePerioder;
    }

    public List<OpphørPeriodeVurderingDto> getVurdertePerioder() {
        return vurdertePerioder;
    }
}
