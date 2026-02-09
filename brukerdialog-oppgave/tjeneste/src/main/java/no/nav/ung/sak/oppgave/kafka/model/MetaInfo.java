package no.nav.ung.sak.oppgave.kafka.model;

public record MetaInfo(
    int version,
    String correlationId,
    String soknadDialogCommitSha
) {
}

