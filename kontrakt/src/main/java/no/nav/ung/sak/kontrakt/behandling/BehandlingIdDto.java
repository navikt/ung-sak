package no.nav.ung.sak.kontrakt.behandling;

import java.util.Objects;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;

/**
 * Referanse til en behandling.
 * Enten {@link #id} eller {@link #behandlingUuid} vil være satt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.PUBLIC_ONLY, fieldVisibility = Visibility.ANY)
@JsonInclude(Include.NON_NULL)
public class BehandlingIdDto {

    public static final String NAME = "behandlingId";

    @JsonProperty(value = NAME, required = true)
    @JsonValue
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String id;

    public BehandlingIdDto(Integer id) {
        this.id = Objects.requireNonNull(id, "id").toString();
    }

    public BehandlingIdDto(Long id) {
        this.id = Objects.requireNonNull(id, "id").toString();
    }

    public BehandlingIdDto(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public BehandlingIdDto(UUID id) {
        this.id = Objects.requireNonNull(id, "id").toString();
    }

    protected BehandlingIdDto() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var other = (BehandlingIdDto) obj;
        return Objects.equals(this.id, other.id);
    }

    /**
     * Denne er kun intern nøkkel, bør ikke eksponeres ut men foreløpig støttes både Long id og UUID id for behandling på grensesnittene.
     */
    @StandardAbacAttributt(StandardAbacAttributtType.BEHANDLING_ID)
    public Long getBehandlingId() {
        return id != null && isLong() ? Long.parseLong(id) : null;
    }

    @StandardAbacAttributt(StandardAbacAttributtType.BEHANDLING_UUID)
    public UUID getBehandlingUuid() {
        return id != null && !isLong() ? UUID.fromString(id) : null;
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @JsonSetter(NAME)
    public void setBehandlingId(String behandlingId) {
        this.id = Objects.requireNonNull(behandlingId, NAME);
        validerLongEllerUuid();
    }

    @Override
    public String toString() {
        return id;
    }

    private boolean isLong() {
        return id.matches("^\\d+$");
    }

    private void validerLongEllerUuid() {
        // valider
        if (isLong()) {
            getBehandlingId();
        } else {
            getBehandlingUuid();
        }
    }
}
