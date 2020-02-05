package no.nav.foreldrepenger.dokumentarkiv.journal;

import java.util.List;

import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.k9.sak.typer.JournalpostId;

public interface JournalTjeneste {

    List<JournalMetadata> hentMetadata(JournalpostId journalpostId);

    ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId);

}
