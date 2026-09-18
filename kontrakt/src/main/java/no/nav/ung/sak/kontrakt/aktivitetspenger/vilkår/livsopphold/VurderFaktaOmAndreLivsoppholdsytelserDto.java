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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FAKTA_OM_ANDRE_LIVSOPPHOLDSYTELSER_KODE)
public class VurderFaktaOmAndreLivsoppholdsytelserDto extends BekreftetAksjonspunktDto {

    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid AndreLivsoppholdsytelserFaktaavklaringPeriodeDto> avklaringer;

    public VurderFaktaOmAndreLivsoppholdsytelserDto() {
        //for jackson
    }

    @JsonCreator
    public VurderFaktaOmAndreLivsoppholdsytelserDto(@JsonProperty("avklaringer") List<AndreLivsoppholdsytelserFaktaavklaringPeriodeDto> avklaringer,
                                                    @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.avklaringer = avklaringer;
    }

    public List<AndreLivsoppholdsytelserFaktaavklaringPeriodeDto> getAvklaringer() {
        return avklaringer;
    }

}
