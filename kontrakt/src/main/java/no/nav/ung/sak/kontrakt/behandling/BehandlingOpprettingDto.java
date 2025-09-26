package no.nav.ung.sak.kontrakt.behandling;


import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.typer.Periode;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BehandlingOpprettingDto {

    @JsonProperty(value = "behandlingType", required = true)
    @NotNull
    private BehandlingType behandlingType;

    @JsonProperty(value = "kanOppretteBehandling")
    private boolean kanOppretteBehandling;

    @JsonProperty(value = "gyldigePerioderPerÅrsak")
    private List<ÅrsakOgPerioderDto> gyldigePerioderPerÅrsak;

    public BehandlingOpprettingDto(BehandlingType behandlingType, boolean kanOppretteBehandling, List<ÅrsakOgPerioderDto> gyldigePerioderPerÅrsak) {
        this.behandlingType = behandlingType;
        this.kanOppretteBehandling = kanOppretteBehandling;
        this.gyldigePerioderPerÅrsak = gyldigePerioderPerÅrsak;
    }

    public BehandlingOpprettingDto() {
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public boolean isKanOppretteBehandling() {
        return kanOppretteBehandling;
    }

    public List<ÅrsakOgPerioderDto> getGyldigePerioderPerÅrsak() {
        return gyldigePerioderPerÅrsak;
    }
}
