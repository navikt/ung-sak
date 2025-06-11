package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.formidling.IdType;

public record InformasjonsbrevMottakerDto(
        @Valid
        @NotNull
        @Size(max = 20)
        @Pattern(regexp = "^\\d+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'"
        )
        @JsonProperty("id")
        String id,

        @Valid
        @NotNull
        @Pattern(regexp = "^[\\p{L}\\p{N}]+$")
        @JsonProperty("type")
        IdType type

) {
}
