package no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for å opprette en inntektsrapporteringsoppgave.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpprettInntektsrapporteringOppgaveDto(

    @JsonProperty(value = "deltakerIdent", required = true)
    @NotNull
    String deltakerIdent,

    @JsonProperty(value = "oppgaveReferanse", required = true)
    @NotNull
    UUID oppgaveReferanse,

    @JsonProperty(value = "fomDato", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate fomDato,

    @JsonProperty(value = "tomDato", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate tomDato,

    @JsonProperty(value = "gjelderDelerAvMåned", required = true)
    boolean gjelderDelerAvMåned,

    @JsonProperty(value = "frist")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime frist
) {
}

