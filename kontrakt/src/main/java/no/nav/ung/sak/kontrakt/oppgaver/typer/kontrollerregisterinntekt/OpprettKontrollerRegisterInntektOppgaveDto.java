package no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for å opprette en oppgave om kontroll av registerinntekt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpprettKontrollerRegisterInntektOppgaveDto(

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

    @JsonProperty(value = "registerInntekter", required = true)
    @NotNull
    @Valid
    RegisterinntektDTO registerInntekter,

    @JsonProperty(value = "gjelderDelerAvMåned", required = true)
    boolean gjelderDelerAvMåned,

    @JsonProperty(value = "frist")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime frist
) {
}

