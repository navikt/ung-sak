package no.nav.k9.sak.kontrakt.behandling;


import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BehandlingOpprettingDto {

    @JsonProperty(value = "behandlingType", required = true)
    @NotNull
    private BehandlingType behandlingType;

    @JsonProperty(value = "kanOppretteBehandling")
    private boolean kanOppretteBehandling;

    public BehandlingOpprettingDto(BehandlingType behandlingType, boolean kanOppretteBehandling) {
        this.behandlingType = behandlingType;
        this.kanOppretteBehandling = kanOppretteBehandling;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public boolean isKanOppretteBehandling() {
        return kanOppretteBehandling;
    }
}
