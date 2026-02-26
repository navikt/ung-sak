package no.nav.ung.sak.kontrakt.oppgaver;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.AktørId;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for migrering av enkelt oppgave.
 */
public record MigrerOppgaveDto(
    @JsonProperty(value = "oppgaveReferanse", required = true)
    @NotNull
    @Valid
    UUID oppgaveReferanse,

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Valid
    AktørId aktørId,

    @JsonProperty(value = "oppgavetype", required = true)
    @NotNull
    OppgaveType oppgavetype,

    @JsonProperty(value = "oppgavetypeData", required = true)
    @NotNull
    @Valid
    OppgavetypeDataDto oppgavetypeData,

    @JsonProperty(value = "bekreftelse")
    @Valid
    BekreftelseDTO bekreftelse,

    @JsonProperty(value = "status", required = true)
    @NotNull
    OppgaveStatus status,

    @JsonProperty(value = "opprettetDato", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    ZonedDateTime opprettetDato,

    @JsonProperty(value = "løstDato")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    ZonedDateTime løstDato,

    @JsonProperty(value = "åpnetDato")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    ZonedDateTime åpnetDato,

    @JsonProperty(value = "lukketDato")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    ZonedDateTime lukketDato,

    @JsonProperty(value = "frist")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    ZonedDateTime frist
) {
}

