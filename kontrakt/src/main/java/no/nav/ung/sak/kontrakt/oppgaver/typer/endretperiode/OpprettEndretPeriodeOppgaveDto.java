package no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for å opprette en oppgave om endret periode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpprettEndretPeriodeOppgaveDto(

    @JsonProperty(value = "deltakerIdent", required = true)
    @NotNull
    String deltakerIdent,

    @JsonProperty(value = "oppgaveReferanse", required = true)
    @NotNull
    UUID oppgaveReferanse,

    @JsonProperty(value = "nyPeriode")
    @Valid
    PeriodeDTO nyPeriode,

    @JsonProperty(value = "forrigePeriode")
    @Valid
    PeriodeDTO forrigePeriode,

    @JsonProperty(value = "endringer", required = true)
    @NotNull
    Set<PeriodeEndringType> endringer,

    @JsonProperty(value = "frist")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime frist
) {
}

