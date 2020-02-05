package no.nav.foreldrepenger.dokumentarkiv.journal.impl;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.journal.InngåendeJournalAdapter;
import no.nav.foreldrepenger.dokumentarkiv.journal.JournalMetadata;
import no.nav.foreldrepenger.dokumentarkiv.journal.JournalTjeneste;
import no.nav.k9.sak.typer.JournalpostId;

@ApplicationScoped
public class JournalTjenesteImpl implements JournalTjeneste {

    private InngåendeJournalAdapter inngaaendeJournalAdapter;

    public JournalTjenesteImpl() {
        // NOSONAR: cdi
    }

    @Inject
    public JournalTjenesteImpl(InngåendeJournalAdapter inngaaendeJournalAdapter) {
        this.inngaaendeJournalAdapter = inngaaendeJournalAdapter;
    }

    @Override
    public List<JournalMetadata> hentMetadata(JournalpostId journalpostId) {
        return inngaaendeJournalAdapter.hentMetadata(journalpostId);
    }

    @Override
    public ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId) {
        return inngaaendeJournalAdapter.hentInngåendeJournalpostHoveddokument(journalpostId);
    }
}
