package no.nav.k9.sak.dokument.arkiv.journal;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.typer.JournalpostId;

@ApplicationScoped
public class JournalTjeneste {

    private InngåendeJournalAdapter inngaaendeJournalAdapter;

    public JournalTjeneste() {
        // y
    }

    @Inject
    public JournalTjeneste(InngåendeJournalAdapter inngaaendeJournalAdapter) {
        this.inngaaendeJournalAdapter = inngaaendeJournalAdapter;
    }

    public List<JournalMetadata> hentMetadata(JournalpostId journalpostId) {
        return inngaaendeJournalAdapter.hentMetadata(journalpostId);
    }

    public ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId) {
        return inngaaendeJournalAdapter.hentInngåendeJournalpostHoveddokument(journalpostId);
    }
}
