package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.*;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Optional;

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

    @JsonProperty(value = "antallDager")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer antallDager;

    @JsonCreator
    public AvklarÅrskvantumDokDto(@JsonProperty(value = "begrunnelse", required = true) @NotNull String begrunnelse,
                                  @JsonProperty(value = "innvilgePeriodene", required = true) @NotNull Boolean innvilgePeriodene,
                                  @JsonProperty(value = "antallDager") Integer antallDager,
                                  @JsonProperty(value = "fortsettBehandling", required = true) @NotNull Boolean fortsettBehandling) {
        super(begrunnelse);
        this.innvilgePeriodene = innvilgePeriodene;
        this.antallDager = antallDager;
        this.fortsettBehandling = fortsettBehandling;
    }

    protected AvklarÅrskvantumDokDto() {
        //
    }

    public Boolean getinnvilgePeriodene() {
        return innvilgePeriodene;
    }

    public Optional<Integer> getAntallDager() {
        return Optional.ofNullable(antallDager);
    }

    public Boolean getfortsettBehandling() {
        return fortsettBehandling;
    }
}
