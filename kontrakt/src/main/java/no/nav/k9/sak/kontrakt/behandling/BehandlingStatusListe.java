package no.nav.k9.sak.kontrakt.behandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.PUBLIC_ONLY, fieldVisibility = Visibility.ANY)
@JsonInclude(Include.NON_NULL)
public class BehandlingStatusListe {

    @JsonProperty(value = "behandlinger", required = true)
    @NotNull
    @Valid
    private List<StatusDto> behandlinger = new ArrayList<>();

    public List<StatusDto> getBehandlinger() {
        return behandlinger;
    }

    @JsonCreator
    public BehandlingStatusListe(@JsonProperty(value = "behandlinger", required = true) @NotNull @Valid List<StatusDto> behandlinger) {
        this.behandlinger = behandlinger;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.PUBLIC_ONLY, fieldVisibility = Visibility.ANY)
    @JsonInclude(Include.NON_NULL)
    public static class StatusDto {

        @JsonProperty(value = "behandlingUuid", required = true)
        @Valid
        @NotNull
        private UUID behandlingUuid;

        @JsonProperty(value = "prosessStatus")
        @Valid
        private AsyncPollingStatus prosessStatus;

        @JsonProperty(value = "behandlingStatus", required = true)
        @Valid
        @NotNull
        private BehandlingStatus behandlingStatus;

        @JsonCreator
        public StatusDto(@JsonProperty(value = "behandlingUuid", required = true) @Valid @NotNull UUID behandlingUuid,
                         @JsonProperty(value = "prosessStatus") @Valid AsyncPollingStatus prosessStatus,
                         @JsonProperty(value = "behandlingStatus", required = true) @Valid BehandlingStatus behandlingStatus) {
            this.behandlingUuid = Objects.requireNonNull(behandlingUuid, "behandlingUuid");
            this.behandlingStatus = Objects.requireNonNull(behandlingStatus, "behandlingStatus");
            this.prosessStatus = prosessStatus;
        }

        public BehandlingStatus getBehandlingStatus() {
            return behandlingStatus;
        }

        public UUID getBehandlingUuid() {
            return behandlingUuid;
        }

        public AsyncPollingStatus getProsessStatus() {
            return prosessStatus;
        }
    }
}
