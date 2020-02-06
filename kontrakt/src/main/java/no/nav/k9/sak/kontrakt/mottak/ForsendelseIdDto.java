package no.nav.k9.sak.kontrakt.mottak;

import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class ForsendelseIdDto {

    public static final String UUID_REGEXP = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @JsonProperty(value = "forsendelseId", required = true)
    @NotNull
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    @Size(max = 36)
    private final String forsendelseId;

    @JsonCreator
    public ForsendelseIdDto(@JsonProperty(value = "forsendelseId", required = true) @NotNull @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") @Size(max = 36) String forsendelseId) {
        this.forsendelseId = forsendelseId;
    }

    public static ForsendelseIdDto fromString(String uuid) {
        return new ForsendelseIdDto(uuid);
    }

    public UUID getForsendelseId() {
        return UUID.fromString(this.forsendelseId);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{forsendelseId='" + this.forsendelseId + '\'' + '}';
    }

}
