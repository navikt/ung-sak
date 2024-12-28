package no.nav.ung.sak.formidling.dokarkiv.dto;

import java.util.List;

public record OpprettJournalpostResponse(
    String journalpostId,
    List<Dokument> dokumenter,
    boolean journalpostferdigstilt,
    String melding
) {
    public boolean erFerdigstilt() {
        return journalpostferdigstilt;
    }

    public record Dokument(
        String dokumentInfoId
    ) {}
}
