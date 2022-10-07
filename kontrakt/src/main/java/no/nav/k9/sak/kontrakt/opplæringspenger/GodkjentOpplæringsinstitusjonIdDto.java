package no.nav.k9.sak.kontrakt.opplæringspenger;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.abac.AbacAttributt;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GodkjentOpplæringsinstitusjonIdDto {

    @JsonValue
    @Valid
    @NotNull
    private UUID godkjentOpplæringsinstitusjonUuid;

    public GodkjentOpplæringsinstitusjonIdDto(String godkjentOpplæringsinstitusjonUuid) {
        this.godkjentOpplæringsinstitusjonUuid = UUID.fromString(Objects.requireNonNull(godkjentOpplæringsinstitusjonUuid));
    }

    @JsonCreator
    public GodkjentOpplæringsinstitusjonIdDto(UUID godkjentOpplæringsinstitusjonUuid) {
        this.godkjentOpplæringsinstitusjonUuid = Objects.requireNonNull(godkjentOpplæringsinstitusjonUuid);
    }

    protected GodkjentOpplæringsinstitusjonIdDto() {
        //
    }

    @AbacAttributt(value = "uuid", masker = true)
    public UUID getUuid() {
        return godkjentOpplæringsinstitusjonUuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var other = (GodkjentOpplæringsinstitusjonIdDto) obj;
        return Objects.equals(this.godkjentOpplæringsinstitusjonUuid, other.godkjentOpplæringsinstitusjonUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(godkjentOpplæringsinstitusjonUuid);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '<' + "" + godkjentOpplæringsinstitusjonUuid + '>';
    }
}
