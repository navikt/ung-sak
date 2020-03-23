package no.nav.k9.sak.web.app.tjenester.historikk;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.historikk.HistorikkInnslagKonverter;
import no.nav.k9.sak.kontrakt.historikk.HistorikkinnslagDto;
import no.nav.k9.sak.typer.JournalpostId;

public class HistorikkInnslagKonverterTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Test
    public void skalSetteDokumentLinksSomUtgåttHvisTomListeAvArkivJournalPost() {
        HistorikkInnslagKonverter konverterer = konverterer();

        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        HistorikkinnslagDto resultat = konverterer.mapFra(historikkinnslag, Collections.emptyList());
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isTrue();
    }

    @Test
    public void skalSetteDokumentLinksSomUtgåttHvisIkkeFinnesMatchendeArkivJournalPost() {
        HistorikkInnslagKonverter konverterer = konverterer();

        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        ArkivJournalPost ikkeMatchendeArkivJournalPost = ArkivJournalPost.Builder.ny()
            .medJournalpostId(new JournalpostId(2L))
            .build();
        HistorikkinnslagDto resultat = konverterer.mapFra(historikkinnslag, Collections.singletonList(ikkeMatchendeArkivJournalPost));
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isTrue();
    }

    @Test
    public void skalSetteDokumentLinksSomIkkeUtgåttHvisFinnesMatchendeArkivJournalPost() {
        HistorikkInnslagKonverter konverterer = konverterer();

        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        ArkivJournalPost ikkeMatchendeArkivJournalPost = ArkivJournalPost.Builder.ny()
            .medJournalpostId(journalpostId)
            .build();
        HistorikkinnslagDto resultat = konverterer.mapFra(historikkinnslag, Collections.singletonList(ikkeMatchendeArkivJournalPost));
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isFalse();
    }

    private HistorikkInnslagKonverter konverterer() {
        return new HistorikkInnslagKonverter();
    }
}
