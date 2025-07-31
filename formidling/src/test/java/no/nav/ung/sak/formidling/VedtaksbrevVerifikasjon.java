package no.nav.ung.sak.formidling;

import no.nav.ung.sak.formidling.scenarioer.BrevScenarioer;

import java.time.LocalDate;

public class VedtaksbrevVerifikasjon {

    private static final String STANDARD_HEADER_FOOTER = """
        Brev for ungdomsprogramytelsen %s \
        Til: %s \
        Fødselsnummer: %s \
        %s\
        Du har rett til å klage \
        Du kan klage på dette vedtaket. \
        Hvis du skal klage, må du gjøre det innen seks uker har gått fra den datoen da du mottok dette vedtaksbrevet. \
        Når du skal klage, kan du skrive en melding til oss på nettsiden vår: nav.no/skriv-til-oss. \
        Pass på at du får tydelig fram hva slags vedtak du klager på, og hvorfor du klager. \
        Du har rett til innsyn \
        Du kan se dokumentene i saken din ved å logge deg inn på nav.no. \
        Trenger du mer informasjon? \
        Du finner mer informasjon på nav.no/ungdomsprogrammet. \
        På nav.no/kontakt kan du chatte eller skrive til oss. \
        Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09:00-15:00. \
        Med vennlig hilsen \
        Nav Arbeid og ytelser \
        %s\
        side av""";

    static String medHeaderOgFooter(String fnr, String body) {
        LocalDate brevdato = LocalDate.now();

        return STANDARD_HEADER_FOOTER
            .formatted(
                BrevTestUtils.brevDatoString(brevdato),
                BrevScenarioer.DEFAULT_NAVN,
                fnr,
                body,
                "Dette er et automatisk behandlet vedtak. ");
    }

    static String medHeaderOgFooterManuell(String fnr, String body) {
        LocalDate brevdato = LocalDate.now();
        return STANDARD_HEADER_FOOTER
            .formatted(
                BrevTestUtils.brevDatoString(brevdato),
                BrevScenarioer.DEFAULT_NAVN,
                fnr,
                body,
                "");
    }
}
