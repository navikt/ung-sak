package no.nav.k9.sak.dokument.arkiv.saf.graphql;

import javax.validation.constraints.NotNull;

public class JournalpostQuery {

    @NotNull
    private String journalpostId;

    public JournalpostQuery(@NotNull String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    @Override
    public String toString() {
        return "JournalpostQuery{" +
            "journalpostId='" + journalpostId + '\'' +
            '}';
    }
}
