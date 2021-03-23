package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.*;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ÅRSKVANTUM_DOK)
public class AvklarÅrskvantumDokDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "fortsettBehandling", required = true)
    @NotNull
    private Boolean fortsettBehandling;

    @JsonProperty(value = "innvilgePeriodene", required = true)
    @NotNull
    private Boolean innvilgePeriodene;

    @JsonCreator
    public AvklarÅrskvantumDokDto(@JsonProperty(value = "begrunnelse", required = true) @NotNull String begrunnelse,
                                  @JsonProperty(value = "innvilgePeriodene", required = true) @NotNull Boolean innvilgePeriodene,
                                  @JsonProperty(value = "fortsettBehandling", required = true) @NotNull Boolean fortsettBehandling) {
        super(begrunnelse);
        this.innvilgePeriodene = innvilgePeriodene;
        this.fortsettBehandling = fortsettBehandling;
    }

    protected AvklarÅrskvantumDokDto() {
        //
    }

    public Boolean getinnvilgePeriodene() {
        return innvilgePeriodene;
    }

    public Boolean getfortsettBehandling() {
        return fortsettBehandling;
    }
}
