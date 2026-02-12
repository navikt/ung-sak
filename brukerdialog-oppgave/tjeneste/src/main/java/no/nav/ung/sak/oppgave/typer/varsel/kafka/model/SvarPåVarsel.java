package no.nav.ung.sak.oppgave.typer.varsel.kafka.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import no.nav.k9.oppgave.OppgaveBekreftelse;

public record SvarPåVarsel(
    String journalpostId,
    @JsonAlias("søknad") OppgaveBekreftelse oppgaveBekreftelse
) {
}

