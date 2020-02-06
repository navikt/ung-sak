package no.nav.k9.sak.kontrakt.medisinsk;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

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

    @JsonProperty(value = "legeerklæring")
    @Valid
    @Size(max = 100)
    private List<Legeerklæring> legeerklæring;

    @JsonProperty(value = "pleiebehov")
    @Valid
    private Pleiebehov pleiebehov;

    private AvklarMedisinskeOpplysningerDto() {
        //For Jackson
    }

    public AvklarMedisinskeOpplysningerDto(List<Legeerklæring> legeerklæringer,
                                           Pleiebehov pleiebehov, String begrunnelse) {
        super(begrunnelse);
        this.legeerklæring = legeerklæringer;
        this.pleiebehov = pleiebehov;
    }

    public List<Legeerklæring> getLegeerklæringer() {
        return legeerklæring;
    }

    public Pleiebehov getPleiebehov() {
        return pleiebehov;
    }
}
