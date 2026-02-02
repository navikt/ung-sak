package no.nav.ung.sak.oppgave.kafka.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.ung.sak.kontrakt.oppgaver.BekreftelseDTO;

/**
 * Record representing the confirmation of an oppgave (task) for ungdomsytelse.
 */
public record UngdomsytelseOppgavebekreftelse(
    String journalpostId,
    @JsonAlias("søknad") OppgaveBekreftelse oppgaveBekreftelse
) {
}

