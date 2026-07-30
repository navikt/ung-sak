package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vedtak.AksjonspunktGodkjenningDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.LOKALKONTOR_BESLUTTER_VILKÅR_KODE)
public class LokalkontorBeslutterVilkårAksjonspunktDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "aksjonspunktGodkjenningDtos")
    @Size(max = 20)
    private Collection<@Valid AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos = new ArrayList<>();

    public LokalkontorBeslutterVilkårAksjonspunktDto() {
        // For Jackson
    }

    public LokalkontorBeslutterVilkårAksjonspunktDto(String begrunnelse, Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos) {
        super(begrunnelse);
        this.aksjonspunktGodkjenningDtos = List.copyOf(aksjonspunktGodkjenningDtos);
    }

    public Collection<AksjonspunktGodkjenningDto> getAksjonspunktGodkjenningDtos() {
        return Collections.unmodifiableCollection(aksjonspunktGodkjenningDtos);
    }

    public void setAksjonspunktGodkjenningDtos(Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos) {
        this.aksjonspunktGodkjenningDtos = aksjonspunktGodkjenningDtos;
    }
}
