package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import no.nav.k9.sak.typer.JournalpostId;

public class MottattKrav {

    private JournalpostId journalpostId;
    private Long behandlingId;

    public MottattKrav(JournalpostId journalpostId, Long behandlingId) {
        this.journalpostId = journalpostId;
        this.behandlingId = behandlingId;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }
}
