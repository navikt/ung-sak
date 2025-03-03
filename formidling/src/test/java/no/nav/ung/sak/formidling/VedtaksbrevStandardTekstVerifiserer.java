package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;

import java.time.LocalDate;

import no.nav.ung.sak.test.util.behandling.UngTestScenario;

public class VedtaksbrevStandardTekstVerifiserer {
    static void verifiserStandardVedtaksbrevTekster(String brevHtml, String fnr, UngTestScenario ungTestscenario) {
        assertThatHtml(brevHtml).containsTextsOnceInSequence(
            BrevUtils.brevDatoString(LocalDate.now()), //vedtaksdato
            "Til: " + ungTestscenario.navn(),
            "Fødselsnummer: " + fnr,
            "Du har rett til å klage",
            "Du kan klage innen 6 uker fra den datoen du mottok vedtaket. Du finner skjema og informasjon på nav.no/klage. ",
            "Du har rett til innsyn",
            "Du kan se dokumentene i saken din ved å logge deg inn på nav.no. ",
            "Trenger du mer informasjon?",
            "Med vennlig hilsen",
            "Nav Arbeid og ytelser"
        ).containsHtmlOnceInSequence(
            "<h2>Du har rett til å klage</h2>",
            "<h2>Du har rett til innsyn</h2>",
            "<h2>Trenger du mer informasjon?</h2>"
        );
    }
}
