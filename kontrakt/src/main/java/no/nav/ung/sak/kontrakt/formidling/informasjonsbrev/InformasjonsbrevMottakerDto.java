package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.formidling.IdType;

/**
 * Request dto for å angi mottaker
 * @param id
 * @param type
 */
public record InformasjonsbrevMottakerDto(
        @Valid
        @NotNull
        @Size(max = 20)
        @Pattern(regexp = "^\\d+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'"
        )
        String id,

        @NotNull
        IdType type

) {
}
