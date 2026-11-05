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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FAKTA_OM_AKTIVITET_KODE)
public class VurderFaktaOmAktivitetDto extends BekreftetAksjonspunktDto {

    /**
     * Fakta-avklaringer om brukers aktivitet per periode.
     */
    @JsonProperty("avklaringer")
    @NotNull
    @Size(min = 1, max = 1)
    private List<@Valid AktivitetFaktaavklaringPeriodeDto> avklaringer;

    public VurderFaktaOmAktivitetDto() {
        //for jackson
    }

    @JsonCreator
    public VurderFaktaOmAktivitetDto(@JsonProperty("avklaringer") List<AktivitetFaktaavklaringPeriodeDto> avklaringer,
                                     @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.avklaringer = avklaringer;
    }

    public List<AktivitetFaktaavklaringPeriodeDto> getAvklaringer() {
        return avklaringer;
    }

}
