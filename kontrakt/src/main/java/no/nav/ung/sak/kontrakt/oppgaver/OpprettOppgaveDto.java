package no.nav.ung.sak.kontrakt.oppgaver;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generell DTO for opprettelse av en brukerdialogoppgave.
 * Oppgavetypen bestemmes av {@link OppgavetypeDataDto}-subtypen i {@code oppgavetypeData}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpprettOppgaveDto(

    @JsonProperty(value = "deltakerIdent", required = true)
    @NotNull
    String deltakerIdent,

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

