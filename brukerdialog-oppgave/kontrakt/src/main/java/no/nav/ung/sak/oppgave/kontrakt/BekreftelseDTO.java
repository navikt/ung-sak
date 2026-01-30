package no.nav.ung.sak.oppgave.kontrakt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for bekreftelse fra bruker på en oppgave.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public record BekreftelseDTO(
    @JsonProperty(value = "harUttalelse", required = true)
    @NotNull
    @Valid
    Boolean harUttalelse,

    @JsonProperty(value = "uttalelseFraBruker")
    String uttalelseFraBruker
) {
}

