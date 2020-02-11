package no.nav.k9.sak.kontrakt.behandling;

import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, creatorVisibility = Visibility.ANY)
public class BehandlingUuidDto {

    public static final String NAME = "behandlingUuid";

    public static final String DESC = "behandlingUUID";

    /**
     * Behandling UUID (nytt alternativ til intern behandlingId. Bør brukes av eksterne systemer).
     */
    @JsonProperty(value = NAME, required = true)
    @Valid
    @NotNull
    private UUID behandlingUuid;

    public BehandlingUuidDto(String behandlingUuid) {
        this.behandlingUuid = UUID.fromString(Objects.requireNonNull(behandlingUuid, NAME));
    }

    @JsonCreator
    public BehandlingUuidDto(@NotNull UUID behandlingUuid) {
        this.behandlingUuid = Objects.requireNonNull(behandlingUuid, NAME);
    }

    protected BehandlingUuidDto() {
        //
    }

    @AbacAttributt(NAME)
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

}
