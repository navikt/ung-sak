package no.nav.ung.sak.kontrakt.oppgaver;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generell DTO for opprettelse av en brukerdialogoppgave.
 * Oppgavetypen bestemmes av {@link OppgavetypeDataDto}-subtypen i {@code oppgavetypeData}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpprettOppgaveDto(

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Valid
    AktørId aktørId,

    @JsonProperty(value = "oppgaveReferanse", required = true)
    @NotNull
    UUID oppgaveReferanse,

    @JsonProperty(value = "oppgavetypeData", required = true)
    @NotNull
    @Valid
    OppgavetypeDataDto oppgavetypeData,

    @JsonProperty(value = "frist")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime frist
) {
}

