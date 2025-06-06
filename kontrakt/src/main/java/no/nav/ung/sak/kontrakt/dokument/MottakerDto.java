package no.nav.ung.sak.kontrakt.dokument;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(
    ignoreUnknown = true
)
@JsonFormat(
    shape = Shape.OBJECT
)
@JsonAutoDetect(
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    fieldVisibility = Visibility.ANY
)
@Deprecated
public class MottakerDto {
    @Valid
    @NotNull
    @Size(
        max = 20
    )
    @Pattern(
        regexp = "^\\d+$",
        message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'"
    )
    public final String id;

    @Valid
    @NotNull
    @Pattern(regexp = "^[\\p{L}\\p{N}]+$")
    public final String type;

    @JsonCreator
    public MottakerDto(@JsonProperty("id") String id, @JsonProperty("type") String type) {
        this.id = id;
        this.type = type;
    }
}
