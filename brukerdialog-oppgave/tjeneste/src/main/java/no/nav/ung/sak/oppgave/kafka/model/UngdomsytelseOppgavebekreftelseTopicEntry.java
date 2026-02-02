package no.nav.ung.sak.oppgave.kafka.model;

/**
 * Record representing a topic entry for UngdomsytelseOppgavebekreftelse events.
 */
public record UngdomsytelseOppgavebekreftelseTopicEntry(
    MetaInfo metadata,
    JournalførtUngdomsytelseOppgavebekreftelse data
) {
}

