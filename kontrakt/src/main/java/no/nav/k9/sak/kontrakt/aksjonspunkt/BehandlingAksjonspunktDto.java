package no.nav.k9.sak.kontrakt.aksjonspunkt;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingAksjonspunktDto {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "ytelseType", required = true)
    private FagsakYtelseType ytelseType;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "fagsakStatus")
    private FagsakStatus fagsakStatus;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "behandlingUuid", required = true)
    @Valid
    private UUID behandlingUuid;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "behandlingType")
    private BehandlingType behandlingType;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "behandlingStatus")
    private BehandlingStatus behandlingStatus;

    @JsonProperty(value = "aksjonspunkter")
    @Size(max = 100)

    @JsonInclude(value = Include.NON_EMPTY)
    List<AksjonspunktDto> aksjonspunkter;

    @JsonCreator
    public BehandlingAksjonspunktDto(@JsonProperty(value = "saksnummer", required = true) Saksnummer saksnummer,
                                     @JsonProperty(value = "ytelseType", required = true) FagsakYtelseType ytelseType,
                                     @JsonProperty(value = "fagsakStatus") FagsakStatus fagsakStatus,
                                     @JsonProperty(value = "behandlingUuid", required = true) UUID behandlingUuid,
                                     @JsonProperty(value = "behandlingType") BehandlingType behandlingType,
                                     @JsonProperty(value = "behandlingStatus") BehandlingStatus behandlingStatus,
                                     @JsonProperty(value = "aksjonspunkter") List<AksjonspunktDto> aksjonspunkter) {
        this.saksnummer = saksnummer;
        this.ytelseType = ytelseType;
        this.fagsakStatus = fagsakStatus;
        this.behandlingUuid = behandlingUuid;
        this.behandlingType = behandlingType;
        this.behandlingStatus = behandlingStatus;
        this.aksjonspunkter = aksjonspunkter;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public FagsakStatus getFagsakStatus() {
        return fagsakStatus;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public BehandlingStatus getBehandlingStatus() {
        return behandlingStatus;
    }

    public List<AksjonspunktDto> getAksjonspunkter() {
        return aksjonspunkter;
    }

}
