package no.nav.ung.sak.oppgave.kafka.model;

/**
 * Record representing metadata information for a topic entry.
 */
public record MetaInfo(
    int version,
    String correlationId,
    String soknadDialogCommitSha
) {
    public MetaInfo {
        if (version == 0) {
            version = 1;
        }
    }

    public MetaInfo(String correlationId, String soknadDialogCommitSha) {
        this(1, correlationId, soknadDialogCommitSha);
    }
}

