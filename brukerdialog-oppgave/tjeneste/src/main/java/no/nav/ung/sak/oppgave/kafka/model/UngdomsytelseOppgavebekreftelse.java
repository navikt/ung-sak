package no.nav.ung.sak.oppgave.kafka.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import no.nav.k9.oppgave.OppgaveBekreftelse;

public record UngdomsytelseOppgavebekreftelse(
    String journalpostId,
    @JsonAlias("søknad") OppgaveBekreftelse oppgaveBekreftelse
) {
}

