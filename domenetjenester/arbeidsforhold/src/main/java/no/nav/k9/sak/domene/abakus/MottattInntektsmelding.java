package no.nav.k9.sak.domene.abakus;

import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.typer.JournalpostId;

public class MottattInntektsmelding {

    private JournalpostId journalpostId;
    private DokumentStatus status;

    public MottattInntektsmelding(JournalpostId journalpostId, DokumentStatus status) {
        this.journalpostId = journalpostId;
        this.status = status;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public DokumentStatus getStatus() {
        return status;
    }
}
