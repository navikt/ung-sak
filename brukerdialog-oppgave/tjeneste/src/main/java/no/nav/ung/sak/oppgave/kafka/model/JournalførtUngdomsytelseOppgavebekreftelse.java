package no.nav.ung.sak.oppgave.kafka.model;

/**
 * Record representing a journaled UngdomsytelseOppgavebekreftelse event.
 */
public record JournalførtUngdomsytelseOppgavebekreftelse(
    UngdomsytelseOppgavebekreftelse journalførtMelding
) {
}

