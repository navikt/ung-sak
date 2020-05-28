package no.nav.k9.sak.kontrakt.uttak;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ÅRSKVANTUM_KVOTE)
public class AvklarÅrskvantumDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "fortsettBehandling", required = true)
    @NotNull
    private Boolean fortsettBehandling;

    @JsonCreator
    public AvklarÅrskvantumDto(@JsonProperty(value = "begrunnelse", required = true) String begrunnelse, @JsonProperty(value = "fortsettBehandling", required = true) @NotNull Boolean fortsettBehandling) {
        super(begrunnelse);
        this.fortsettBehandling = fortsettBehandling;
    }

    protected AvklarÅrskvantumDto() {
        //
    }

    public Boolean getfortsettBehandling() {
        return fortsettBehandling;
    }
}
