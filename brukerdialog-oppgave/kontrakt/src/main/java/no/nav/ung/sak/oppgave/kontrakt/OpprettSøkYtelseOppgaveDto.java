package no.nav.ung.sak.oppgave.kontrakt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.sak.felles.abac.StandardAbacAttributt;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for opprettelse av søk ytelse oppgave.
 * Brukes når veileder/saksbehandler skal opprette en oppgave som ber bruker om å søke ytelse.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpprettSøkYtelseOppgaveDto(
    @JsonProperty(value = "aktørId", required = true)
    @StandardAbacAttributt(value = StandardAbacAttributtType.AKTØR_ID)
    @NotNull
    @Valid
    String aktørId,

    @JsonProperty(value = "fomDato", required = true)
    @NotNull
    LocalDate fomDato,

    @JsonProperty(value = "oppgaveReferanse")
    @Valid
    UUID oppgaveReferanse
) {
}

