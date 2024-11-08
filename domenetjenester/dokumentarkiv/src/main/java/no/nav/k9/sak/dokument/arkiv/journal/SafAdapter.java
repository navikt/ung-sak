package no.nav.k9.sak.dokument.arkiv.journal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;

@Deprecated
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
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());
        var projection = new JournalpostResponseProjection().kanal();

        var journalpost = safTjeneste.hentJournalpostInfo(query, projection);

        ArkivJournalPost arkivJournalPost = ArkivJournalPost.Builder.ny()
            .medKanalreferanse(journalpost.getKanal().name())
            .build();

        return arkivJournalPost;
    }
}
