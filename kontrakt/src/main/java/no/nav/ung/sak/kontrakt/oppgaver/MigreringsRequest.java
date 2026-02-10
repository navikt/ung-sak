package no.nav.ung.sak.kontrakt.oppgaver;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request for migrering av brukerdialogoppgaver fra annen applikasjon.
 */
public record MigreringsRequest(
    @JsonProperty(value = "oppgaver", required = true)
    @NotNull
    @Size(min = 1, max = 1000)
    List<@NotNull @Valid MigrerOppgaveDto> oppgaver
) {
}

