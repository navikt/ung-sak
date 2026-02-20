package no.nav.ung.sak.kontrakt.oppgaver;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for oppgave med all nødvendig informasjon for visning og håndtering.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public record BrukerdialogOppgaveDto(
    @JsonProperty(value = "oppgaveReferanse", required = true)
    @NotNull
    @Valid
    UUID oppgaveReferanse,

    @JsonProperty(value = "oppgavetype", required = true)
    @NotNull
    OppgaveType oppgavetype,

    @JsonProperty(value = "oppgavetypeData", required = true)
    @NotNull
    OppgavetypeDataDto oppgavetypeData,

    @JsonProperty(value = "bekreftelse")
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

