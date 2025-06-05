package no.nav.ung.sak.web.app.tjenester.historikk;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import no.nav.ung.kodeverk.historikk.HistorikkAktør;
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
        var historikkinnslag = new Historikkinnslag.Builder();
        historikkinnslag.medDokumenter(Collections.singletonList(lenke));
        historikkinnslag.medAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.medFagsakId(1L);
        historikkinnslag.medTittel("Tittel");
        HistorikkinnslagDto resultat = konverterer.map(historikkinnslag.build(), Collections.emptyList());
        assertThat(resultat.dokumenter().get(0).isUtgått()).isTrue();
    }

    @Test
    public void skalSetteDokumentLinksSomUtgåttHvisIkkeFinnesMatchendeArkivJournalPost() {
        HistorikkInnslagKonverter konverterer = konverterer();

        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        var historikkinnslag = new Historikkinnslag.Builder();
        historikkinnslag.medDokumenter(Collections.singletonList(lenke));
        historikkinnslag.medAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.medFagsakId(1L);
        historikkinnslag.medTittel("Tittel");
        var ikkeMatchendeJournalpostId = new JournalpostId(2L);
        HistorikkinnslagDto resultat = konverterer.map(historikkinnslag.build(), Collections.singletonList(ikkeMatchendeJournalpostId));
        assertThat(resultat.dokumenter().get(0).isUtgått()).isTrue();
    }

    @Test
    public void skalSetteDokumentLinksSomIkkeUtgåttHvisFinnesMatchendeArkivJournalPost() {
        HistorikkInnslagKonverter konverterer = konverterer();

        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        var historikkinnslag = new Historikkinnslag.Builder();
        historikkinnslag.medAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.medFagsakId(1L);
        historikkinnslag.medTittel("Tittel");
        historikkinnslag.medDokumenter(Collections.singletonList(lenke));
        HistorikkinnslagDto resultat = konverterer.map(historikkinnslag.build(), Collections.singletonList(journalpostId));
        assertThat(resultat.dokumenter().get(0).isUtgått()).isFalse();
    }

    private HistorikkInnslagKonverter konverterer() {
        return new HistorikkInnslagKonverter(null);
    }
}
