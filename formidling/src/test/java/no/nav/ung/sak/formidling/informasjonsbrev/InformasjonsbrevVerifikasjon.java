package no.nav.ung.sak.formidling.informasjonsbrev;

import no.nav.ung.sak.formidling.BrevTestUtils;
import no.nav.ung.sak.formidling.scenarioer.BrevScenarioerUtils;

import java.time.LocalDate;

public class InformasjonsbrevVerifikasjon {

    private static final String STANDARD_HEADER_FOOTER = """
        Brev for ungdomsprogramytelsen %s \
        Til: %s \
        Fødselsnummer: %s \
        %s\
        Med vennlig hilsen \
        Nav Arbeid og ytelser \
        side av""";

    static String medHeaderOgFooter(String fnr, String body) {
        LocalDate brevdato = LocalDate.now();

        return STANDARD_HEADER_FOOTER
            .formatted(
                BrevTestUtils.brevDatoString(brevdato),
                BrevScenarioerUtils.DEFAULT_NAVN,
                fnr,
                body);
    }

}
