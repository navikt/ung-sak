package no.nav.k9.sak.kontrakt.mottak;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

    @JsonProperty(value = "forsendelseId", required = true)
    @NotNull
    @Valid
    private UUID forsendelseId;

    @JsonCreator
    public ForsendelseIdDto(@NotNull @Valid @JsonProperty(value = "forsendelseId", required = true) UUID forsendelseId) {
        this.forsendelseId = forsendelseId;
    }

    public static ForsendelseIdDto fromString(String uuid) {
        return new ForsendelseIdDto(UUID.fromString(uuid));
    }

    public UUID getForsendelseId() {
        return this.forsendelseId;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{forsendelseId='" + this.forsendelseId + '\'' + '}';
    }

}
