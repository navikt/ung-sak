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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FAKTA_OM_BISTAND_KODE)
public class VurderFaktaOmBistandDto extends BekreftetAksjonspunktDto {

    /**
     * Fakta-avklaringer om brukers bistandsbehov per periode.
     */
    @JsonProperty("avklaringer")
    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid BistandFaktaavklaringPeriodeDto> avklaringer;

    public VurderFaktaOmBistandDto() {
        //for jackson
    }

    @JsonCreator
    public VurderFaktaOmBistandDto(@JsonProperty("avklaringer") List<BistandFaktaavklaringPeriodeDto> avklaringer,
                                   @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.avklaringer = avklaringer;
    }

    public List<BistandFaktaavklaringPeriodeDto> getAvklaringer() {
        return avklaringer;
    }

}
