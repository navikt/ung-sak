package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FAKTA_OM_BOSTED)
public class VurderFaktaOmBostedDto extends BekreftetAksjonspunktDto {

    /**
     * Fakta-avklaringer om brukers bosted per periode.
     * Saksbehandler fyller inn om bruker er bosatt i Trondheim for hvert skjæringstidspunkt.
     */
    @JsonProperty("avklaringer")
    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid BostedFaktaavklaringPeriodeDto> avklaringer;


    public VurderFaktaOmBostedDto() {
        //for jackson
    }

    @JsonCreator
    public VurderFaktaOmBostedDto(@JsonProperty("avklaringer") List<BostedFaktaavklaringPeriodeDto> avklaringer,
                                  @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.avklaringer = avklaringer;
    }

    public List<BostedFaktaavklaringPeriodeDto> getAvklaringer() {
        return avklaringer;
    }

}
