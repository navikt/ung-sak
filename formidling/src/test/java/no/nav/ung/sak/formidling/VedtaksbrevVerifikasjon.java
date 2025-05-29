package no.nav.ung.sak.formidling;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;

public class VedtaksbrevVerifikasjon {


    static String medHeaderOgFooter(String fnr, String body) {
        LocalDate brevdato = LocalDate.now();
        return """
                   Brev for ungdomsytelsen %s \
                   Til: %s \
                   Fødselsnummer: %s \
                   %s\
                   Du har rett til å klage Du kan klage innen 6 uker fra den datoen du mottok vedtaket. \
                   Du finner skjema og informasjon på nav.no/klage. \
                   Du har rett til innsyn \
                   Du kan se dokumentene i saken din ved å logge deg inn på nav.no. \
                   Trenger du mer informasjon? \
                   Du finner mer informasjon på nav.no/ungdomsprogrammet. \
                   På nav.no/kontakt kan du chatte eller skrive til oss. \
                   Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09:00-15:00. \
                   Med vennlig hilsen \
                   Nav Arbeid og ytelser \
                   side av""".formatted(BrevTestUtils.brevDatoString(brevdato), BrevScenarioer.DEFAULT_NAVN, fnr, body);
    }


    static void verifiserStandardOverskrifter(String brevHtml) {
        assertThatHtml(brevHtml).containsHtmlSubSequenceOnce(
            "<h2>Du har rett til å klage</h2>",
            "<h2>Du har rett til innsyn</h2>",
            "<h2>Trenger du mer informasjon?</h2>"
        );
    }
}
