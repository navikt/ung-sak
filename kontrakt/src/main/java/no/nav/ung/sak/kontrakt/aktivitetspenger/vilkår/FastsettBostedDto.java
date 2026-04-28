package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_BOSTEDVILKÅR_KODE)
public class FastsettBostedDto extends BekreftetAksjonspunktDto {

    @JsonProperty("avklaringer")
    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid FastsettBostedPeriodeDto> avklaringer;

    public FastsettBostedDto() {
        // for jackson
    }

    @JsonCreator
    public FastsettBostedDto(@JsonProperty("avklaringer") List<FastsettBostedPeriodeDto> avklaringer,
                              @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.avklaringer = avklaringer;
    }

    public List<FastsettBostedPeriodeDto> getAvklaringer() {
        return avklaringer;
    }
}
