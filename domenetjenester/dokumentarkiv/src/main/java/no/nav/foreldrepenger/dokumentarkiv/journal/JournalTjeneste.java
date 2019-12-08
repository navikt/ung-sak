package no.nav.foreldrepenger.dokumentarkiv.journal;

import java.util.List;

import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public interface JournalTjeneste {

    List<JournalMetadata> hentMetadata(JournalpostId journalpostId);

    ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId);

}
