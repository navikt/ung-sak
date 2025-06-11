package no.nav.ung.sak.formidling;

import java.time.LocalDate;

public class InformasjonsbrevVerifikasjon {

    private static final String STANDARD_HEADER_FOOTER = """
        Brev for ungdomsytelsen %s \
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
                BrevScenarioer.DEFAULT_NAVN,
                fnr,
                body);
    }

}
