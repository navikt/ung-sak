package no.nav.ung.sak.web.app.tjenester.historikk;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.ung.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.ung.sak.historikk.HistorikkInnslagKonverter;
import no.nav.ung.sak.kontrakt.historikk.HistorikkinnslagDto;
import no.nav.ung.sak.typer.JournalpostId;

public class HistorikkInnslagKonverterTest {

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
