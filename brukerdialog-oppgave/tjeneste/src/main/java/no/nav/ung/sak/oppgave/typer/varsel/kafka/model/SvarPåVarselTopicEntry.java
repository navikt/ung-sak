package no.nav.ung.sak.oppgave.typer.varsel.kafka.model;

public record SvarPåVarselTopicEntry(
    MetaInfo metadata,
    JournalførtSvarPåVarsel data
) {
}

