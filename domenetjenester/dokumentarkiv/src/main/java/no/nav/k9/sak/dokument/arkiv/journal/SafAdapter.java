package no.nav.k9.sak.dokument.arkiv.journal;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.dokument.arkiv.saf.SafTjenesteObsolete;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.saf.JournalpostQueryRequest;
import no.nav.saf.JournalpostResponseProjection;
import no.nav.vedtak.felles.integrasjon.saf.SafTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class SafAdapter {

    private SafTjeneste safTjeneste;
    private SafTjenesteObsolete safTjenesteObsolete;
    private Boolean toggletNySafKlient;

    SafAdapter() {
        // for CDI proxy
    }

    @Inject
    public SafAdapter(SafTjeneste safTjeneste,
                      SafTjenesteObsolete safTjenesteObsolete,
                      @KonfigVerdi(value = "SAF_NY_FELLESKLIENT", defaultVerdi = "false") Boolean toggletNySafKlient) {
        this.safTjeneste = safTjeneste;
        this.safTjenesteObsolete = safTjenesteObsolete;
        this.toggletNySafKlient = toggletNySafKlient;
    }

    public ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId) {
        String kanal;
        if (toggletNySafKlient) {
            var query = new JournalpostQueryRequest();
            query.setJournalpostId(journalpostId.getVerdi());
            JournalpostResponseProjection projection = new JournalpostResponseProjection().kanal();
            var journalpost = safTjeneste.hentJournalpostInfo(query, projection);
            kanal = journalpost.getKanal().name();
        } else {
            no.nav.k9.sak.dokument.arkiv.saf.graphql.JournalpostQuery query = new no.nav.k9.sak.dokument.arkiv.saf.graphql.JournalpostQuery(journalpostId.getVerdi());
            var journalpost = safTjenesteObsolete.hentJournalpostInfo(query);
            kanal = journalpost.getKanal();
        }

        ArkivJournalPost arkivJournalPost = ArkivJournalPost.Builder.ny()
            .medKanalreferanse(kanal) // Eneste feltet som er i bruk - kunne vært forenklet en del
            .build();

        return arkivJournalPost;
    }
}
