package no.nav.k9.sak.kontrakt.uttak;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.person.NorskIdentDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ÅRSKVANTUM_KVOTE)
public class AvklarÅrskvantumDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "fortsettBehandling", required = true)
    @NotNull
    private Boolean fortsettBehandling;

    @JsonProperty(value = "innvilgePeriodene")
    private Boolean innvilgePeriodene;

    @JsonProperty(value = "antallDager")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer antallDager;

    @JsonProperty(value = "fosterbarn", required = true)
    @Valid
    @Size(max = 100)
    private List<NorskIdentDto> fosterbarn;

    @JsonCreator
    public AvklarÅrskvantumDto(@JsonProperty(value = "begrunnelse", required = true) String begrunnelse,
                               @JsonProperty(value = "innvilgePeriodene") @NotNull Boolean innvilgePeriodene,
                               @JsonProperty(value = "antallDager") Integer antallDager,
                               @JsonProperty(value = "fortsettBehandling", required = true) Boolean fortsettBehandling,
                               @JsonProperty(value = "fosterbarn") List<NorskIdentDto> fosterbarn) {
        super(begrunnelse);
        this.innvilgePeriodene = innvilgePeriodene;
        this.antallDager = antallDager;
        this.fortsettBehandling = fortsettBehandling;
        this.fosterbarn = fosterbarn;
    }

    protected AvklarÅrskvantumDto() {
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

    public List<NorskIdentDto> getFosterbarn() {
        return fosterbarn;
    }
}
