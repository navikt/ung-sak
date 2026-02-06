package no.nav.ung.sak.oppgave.kafka.model;

public record UngdomsytelseOppgavebekreftelseTopicEntry(
    MetaInfo metadata,
    JournalførtUngdomsytelseOppgavebekreftelse data
) {
}

