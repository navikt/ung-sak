package no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for å opprette en oppgave om endret startdato.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpprettEndretStartdatoOppgaveDto(

    @JsonProperty(value = "deltakerIdent", required = true)
    @NotNull
    String deltakerIdent,

    @JsonProperty(value = "oppgaveReferanse", required = true)
    @NotNull
    UUID oppgaveReferanse,

    @JsonProperty(value = "nyStartdato", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate nyStartdato,

    @JsonProperty(value = "forrigeStartdato", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate forrigeStartdato,

    @JsonProperty(value = "frist")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime frist
) {
}

