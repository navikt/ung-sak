package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.sak.kontrakt.Patterns;

/**
 *
 * @param overskrift for brevet
 * @param brødtekst for brevet
 */
public record GenereltFritekstBrevDto(
    @Valid
    @NotNull
    @Size(max = 200)
    @Pattern(regexp = Patterns.FRITEKSTBREV, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty("overskrift")
    String overskrift,

    @Valid
    @NotNull
    @Size(max = 100000)
    @Pattern(regexp = Patterns.FRITEKSTBREV, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty("brødtekst")
    String brødtekst) {
}
