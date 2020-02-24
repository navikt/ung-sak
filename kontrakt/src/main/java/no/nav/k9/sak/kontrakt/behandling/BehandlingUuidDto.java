package no.nav.k9.sak.kontrakt.behandling;

import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.abac.AbacAttributt;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingUuidDto {

    public static final String DESC = "behandlingUUID";

    public static final String NAME = "behandlingUuid";

    /**
     * Behandling UUID (nytt alternativ til intern behandlingId. Bør brukes av eksterne systemer).
     */
    @JsonValue
    @Valid
    @NotNull
    private UUID behandlingUuid;

    public BehandlingUuidDto(String behandlingUuid) {
        this.behandlingUuid = UUID.fromString(Objects.requireNonNull(behandlingUuid, NAME));
    }

    @JsonCreator
    public BehandlingUuidDto(UUID behandlingUuid) {
        this.behandlingUuid = Objects.requireNonNull(behandlingUuid, NAME);
    }

    protected BehandlingUuidDto() {
        //
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var other = (BehandlingUuidDto) obj;
        return Objects.equals(this.behandlingUuid, other.behandlingUuid);
    }

    @AbacAttributt(NAME)
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingUuid);
    }

    @Override
    public String toString() {
        return String.valueOf(behandlingUuid);
    }
}
