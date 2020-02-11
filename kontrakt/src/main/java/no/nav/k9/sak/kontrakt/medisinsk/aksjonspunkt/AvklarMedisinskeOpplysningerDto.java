package no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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

    @JsonProperty(value = "legeerklæring", required = true)
    @Valid
    @Size(max = 100)
    @NotNull
    private List<Legeerklæring> legeerklæring;

    @JsonProperty(value = "pleiebehov", required = true)
    @Valid
    @NotNull
    private Pleiebehov pleiebehov;

    public AvklarMedisinskeOpplysningerDto(List<Legeerklæring> legeerklæringer,
                                           Pleiebehov pleiebehov,
                                           String begrunnelse) {
        super(begrunnelse);
        this.legeerklæring = legeerklæringer;
        this.pleiebehov = pleiebehov;
    }

    protected AvklarMedisinskeOpplysningerDto() {
        //
    }

    public List<Legeerklæring> getLegeerklæring() {
        return Collections.unmodifiableList(legeerklæring);
    }

    public Pleiebehov getPleiebehov() {
        return pleiebehov;
    }

    public void setLegeerklæring(List<Legeerklæring> legeerklæring) {
        this.legeerklæring = List.copyOf(legeerklæring);
    }

    public void setPleiebehov(Pleiebehov pleiebehov) {
        this.pleiebehov = pleiebehov;
    }
}
