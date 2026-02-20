package no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for å opprette en oppgave om endret sluttdato.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpprettEndretSluttdatoOppgaveDto(

    @JsonProperty(value = "deltakerIdent", required = true)
    @NotNull
    String deltakerIdent,

    @JsonProperty(value = "oppgaveReferanse", required = true)
    @NotNull
    UUID oppgaveReferanse,

    @JsonProperty(value = "nySluttdato", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate nySluttdato,

    @JsonProperty(value = "forrigeSluttdato")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate forrigeSluttdato,

    @JsonProperty(value = "frist")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime frist
) {
}

