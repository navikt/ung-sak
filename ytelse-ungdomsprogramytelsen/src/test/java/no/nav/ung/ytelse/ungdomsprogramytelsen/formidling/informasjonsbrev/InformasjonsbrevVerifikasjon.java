package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.informasjonsbrev;

import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.BrevTestUtils;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.BrevScenarioerUtils;

import java.time.LocalDate;

public class InformasjonsbrevVerifikasjon {

    private static final String STANDARD_HEADER_FOOTER = """
        Brev for ungdomsprogramytelsen %s \
        Til: %s \
        Fødselsnummer: %s \
        %s\
        Med vennlig hilsen \
        Nav Arbeid og ytelser \
        %s side av""";

    static String medHeaderOgFooter(String fnr, String body) {
        LocalDate brevdato = LocalDate.now();

        return STANDARD_HEADER_FOOTER
            .formatted(
                BrevTestUtils.brevDatoString(brevdato),
                BrevScenarioerUtils.DEFAULT_NAVN,
                fnr,
                body,
                BrevScenarioerUtils.DEFAULT_SAKSBEHANDLER_NAVN);
    }

}
