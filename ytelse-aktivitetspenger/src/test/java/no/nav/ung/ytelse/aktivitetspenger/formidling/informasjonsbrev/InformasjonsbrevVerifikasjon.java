package no.nav.ung.ytelse.aktivitetspenger.formidling.informasjonsbrev;

import no.nav.ung.ytelse.aktivitetspenger.formidling.BrevTestUtils;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils;

import java.time.LocalDate;

public class InformasjonsbrevVerifikasjon {

    private static final String STANDARD_HEADER_FOOTER = """
        Brev for aktivitetspenger %s \
        Til: %s \
        Fødselsnummer: %s \
        %s\
        Med vennlig hilsen \
        Nav \
        %s \
        side av""";

    static String medHeaderOgFooter(String fnr, String body) {
        LocalDate brevdato = LocalDate.now();

        return STANDARD_HEADER_FOOTER
            .formatted(
                BrevTestUtils.brevDatoString(brevdato),
                AktivitetspengerBrevScenarioerUtils.DEFAULT_NAVN,
                fnr,
                body,
                AktivitetspengerBrevScenarioerUtils.NAV_SENTRAL_SAKSBEHANDLER_NAVN);
    }

}
