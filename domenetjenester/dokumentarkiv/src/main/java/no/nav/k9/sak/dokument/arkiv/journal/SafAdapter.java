package no.nav.k9.sak.dokument.arkiv.journal;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.dokument.arkiv.saf.SafTjeneste;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.JournalpostQuery;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Journalpost;
import no.nav.k9.sak.typer.JournalpostId;

@ApplicationScoped
public class SafAdapter {

    private SafTjeneste safTjeneste;

    SafAdapter() {
        // for CDI proxy
    }

    @Inject
    public SafAdapter(SafTjeneste safTjeneste) {
        this.safTjeneste = safTjeneste;
    }

    public ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId) {
        JournalpostQuery query = new JournalpostQuery(journalpostId.getVerdi());

        Journalpost journalpost = safTjeneste.hentJournalpostInfo(query);

        ArkivJournalPost arkivJournalPost = ArkivJournalPost.Builder.ny()
            .medKanalreferanse(journalpost.getKanal()) // Eneste feltet som er i bruk - kunne vært forenklet en del
            .build();

        return arkivJournalPost;
    }
}
