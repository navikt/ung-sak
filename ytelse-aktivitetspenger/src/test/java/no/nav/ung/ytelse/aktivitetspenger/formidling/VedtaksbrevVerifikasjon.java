package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils;

import java.time.LocalDate;

public class VedtaksbrevVerifikasjon {

    private static final String STANDARD_HEADER_FOOTER = """
        Brev for aktivitetspenger %s \
        Til: %s \
        Fødselsnummer: %s \
        %s\
        Du har rett til å klage \
        Du kan klage på dette vedtaket. \
        Hvis du skal klage, må du gjøre det innen seks uker har gått fra den datoen da du mottok dette vedtaksbrevet. \
        Du finner skjema og informasjon på nav.no/klage#aktivitetspenger. \
        Pass på at du får tydelig fram hva slags vedtak du klager på, og hvorfor du klager. \
        Du har rett til innsyn \
        Du kan se dokumentene i saken din ved å logge deg inn på nav.no. \
        Trenger du mer informasjon? \
        Du finner mer informasjon på nav.no/aktivitetspenger. \
        På nav.no/kontakt kan du chatte eller skrive til oss. \
        Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 33, hverdager 09:00-15:00. \
        Med vennlig hilsen \
        Nav \
        %s \
        side av""";

    public static final String HILSEN_SENTRAL = hilsen(
        AktivitetspengerBrevScenarioerUtils.NAV_SENTRAL_SAKSBEHANDLER_NAVN, AktivitetspengerBrevScenarioerUtils.NAV_SENTRAL_BESLUTTER_NAVN);
    public static final String HILSEN_NAV_KONTOR = hilsen(
        AktivitetspengerBrevScenarioerUtils.NAV_KONTOR_SAKSBEHANDLER_NAVN, AktivitetspengerBrevScenarioerUtils.NAV_KONTOR_BESLUTTER_NAVN);
    public static final String HILSEN_NAV_KONTOR_OG_SENTRAL = hilsen(
        AktivitetspengerBrevScenarioerUtils.NAV_KONTOR_SAKSBEHANDLER_NAVN, AktivitetspengerBrevScenarioerUtils.NAV_KONTOR_BESLUTTER_NAVN,
        AktivitetspengerBrevScenarioerUtils.NAV_SENTRAL_SAKSBEHANDLER_NAVN, AktivitetspengerBrevScenarioerUtils.NAV_SENTRAL_BESLUTTER_NAVN);

    private static String hilsen(String... navn) {
        return "Med vennlig hilsen Nav " + String.join(" ", navn) + " side av";
    }

    public static String medHeaderOgFooter(String fnr, String body) {
        LocalDate brevdato = LocalDate.now();

        return STANDARD_HEADER_FOOTER
            .formatted(
                BrevTestUtils.brevDatoString(brevdato),
                AktivitetspengerBrevScenarioerUtils.DEFAULT_NAVN,
                fnr,
                body,
                "Dette er et automatisk behandlet vedtak.");
    }

    public static String medHeaderOgFooterManuell(String fnr, String body) {
        LocalDate brevdato = LocalDate.now();
        return STANDARD_HEADER_FOOTER
            .formatted(
                BrevTestUtils.brevDatoString(brevdato),
                AktivitetspengerBrevScenarioerUtils.DEFAULT_NAVN,
                fnr,
                body,
                AktivitetspengerBrevScenarioerUtils.NAV_SENTRAL_SAKSBEHANDLER_NAVN + " " + AktivitetspengerBrevScenarioerUtils.NAV_SENTRAL_BESLUTTER_NAVN);
    }

    public static String medHeaderOgFooterNavKontor(String fnr, String body) {
        LocalDate brevdato = LocalDate.now();
        return STANDARD_HEADER_FOOTER
            .formatted(
                BrevTestUtils.brevDatoString(brevdato),
                AktivitetspengerBrevScenarioerUtils.DEFAULT_NAVN,
                fnr,
                body,
                AktivitetspengerBrevScenarioerUtils.NAV_KONTOR_SAKSBEHANDLER_NAVN + " " + AktivitetspengerBrevScenarioerUtils.NAV_KONTOR_BESLUTTER_NAVN);
    }
}
