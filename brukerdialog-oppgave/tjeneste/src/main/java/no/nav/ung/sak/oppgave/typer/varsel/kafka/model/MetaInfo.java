package no.nav.ung.sak.oppgave.typer.varsel.kafka.model;

public record MetaInfo(
    int version,
    String correlationId,
    String soknadDialogCommitSha
) {
}

