package no.nav.k9.sak.kontrakt.behandling;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.abac.AbacAttributt;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingUuidDto {

    public static final String NAME = "uuid";

    public static final String DESC = "behandlingUUID";

    /**
     * Behandling UUID (nytt alternativ til intern behandlingId. Bør brukes av eksterne systemer).
     */
    @JsonProperty(value = "behandlingUuid", required = true)
    @NotNull
    @Valid
    private UUID behandlingUuid;

    @JsonCreator
    public BehandlingUuidDto(@JsonProperty(value = "behandlingUuid", required = true) @NotNull String behandlingUuid) {
        this.behandlingUuid = UUID.fromString(behandlingUuid);
    }

    public BehandlingUuidDto(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

}
