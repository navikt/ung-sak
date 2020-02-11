package no.nav.k9.sak.kontrakt.behandling;

import java.util.Objects;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import no.nav.k9.abac.AbacAttributt;

/**
 * Referanse til en behandling.
 * Enten {@link #id} eller {@link #behandlingUuid} vil være satt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.PUBLIC_ONLY, fieldVisibility = Visibility.ANY)
@JsonInclude(Include.NON_NULL)
public class BehandlingIdDto {

    public static final String NAME = "behandlingId";

    @JsonProperty(value = NAME, required = true)
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String id;

    public BehandlingIdDto(Integer id) {
        this.id = Objects.requireNonNull(id, "id").toString();
    }

    public BehandlingIdDto(Long id) {
        this.id = Objects.requireNonNull(id, "id").toString();
    }

    public BehandlingIdDto(UUID id) {
        this.id = Objects.requireNonNull(id, "id").toString();
    }

    public BehandlingIdDto() {
    }

    @JsonCreator
    public BehandlingIdDto(@Size(max = 50) @NotNull @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String id) {
        this.id = id;
    }

    @JsonSetter(NAME)
    public void setBehandlingId(String behandlingId) {
        this.id = Objects.requireNonNull(behandlingId, NAME);
        validerLongEllerUuid();
    }

    private void validerLongEllerUuid() {
        // valider
        if (isLong()) {
            getBehandlingId();
        } else {
            getBehandlingUuid();
        }
    }

    /**
     * Denne er kun intern nøkkel, bør ikke eksponeres ut men foreløpig støttes både Long id og UUID id for behandling på grensesnittene.
     */
    @AbacAttributt(NAME)
    public Long getBehandlingId() {
        return id != null && isLong() ? Long.parseLong(id) : null;
    }

    private boolean isLong() {
        return id.matches("^\\d+$");
    }

    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return id != null && !isLong() ? UUID.fromString(id) : null;
    }

    public String getId() {
        return id;
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

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
