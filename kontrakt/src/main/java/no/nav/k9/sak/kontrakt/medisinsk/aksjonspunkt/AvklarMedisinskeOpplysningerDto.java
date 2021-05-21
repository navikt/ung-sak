package no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE)
public class AvklarMedisinskeOpplysningerDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "ikkeVentPåGodkjentLegeerklæring", required = false)
    @NotNull
    private Boolean ikkeVentPåGodkjentLegeerklæring;

    public AvklarMedisinskeOpplysningerDto(String begrunnelse, boolean ikkeVentPåGodkjentLegeerklæring) {
        super(begrunnelse);
        this.ikkeVentPåGodkjentLegeerklæring = ikkeVentPåGodkjentLegeerklæring;
    }

    public AvklarMedisinskeOpplysningerDto() {
        //
    }

    public boolean isIkkeVentPåGodkjentLegeerklæring() {
        return ikkeVentPåGodkjentLegeerklæring != null && ikkeVentPåGodkjentLegeerklæring;
    }
}
