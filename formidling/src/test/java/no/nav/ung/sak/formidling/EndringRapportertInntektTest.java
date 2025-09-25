package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.scenarioer.EndringInntektScenarioer;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringRapportertInntektTest extends AbstractVedtaksbrevInnholdByggerTest {

    EndringRapportertInntektTest() {
        super(1, "Vi har endret ungdomsprogramytelsen din");
    }

    @DisplayName("Endringsbrev med periode, innrapportert inntekt, reduksjon og utbetaling")
    @Test
    void standardEndringRapportertInntekt() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = EndringInntektScenarioer.endringMedInntektPå10k_19år(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsprogramytelsen din " +
                "Du får 8 326 kroner i ungdomsprogramytelse for perioden fra 1. januar 2025 til 31. januar 2025. " +
                "Pengene får du utbetalt innen fire dager. " +
                "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
                "Derfor har vi redusert ungdomsprogramytelsen din med et beløp som tilsvarer 66 prosent av inntekten din. " +
                standardTekstEndringInntekt());

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelsen din</h1>"
            );
    }

    @DisplayName("Endringsbrev med flere perioder")
    @Test
    void melder_inntekt_for_flere_mnd() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = EndringInntektScenarioer.endringMedInntektPå10k_flere_mnd_19år(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsprogramytelsen din " +
                "Du får 8 326 kroner i ungdomsprogramytelse for perioden fra 1. januar 2025 til 31. januar 2025. " +
                "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
                "Derfor har vi redusert ungdomsprogramytelsen din med et beløp som tilsvarer 66 prosent av inntekten din. " +
                "Du får 6 380 kroner i ungdomsprogramytelse for perioden fra 1. februar 2025 til 28. februar 2025. " +
                "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
                "Derfor har vi redusert ungdomsprogramytelsen din med et beløp som tilsvarer 66 prosent av inntekten din. " +
                "Pengene får du utbetalt innen fire dager. " +
                standardTekstEndringInntekt());

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelsen din</h1>"
            );
    }

    @DisplayName("Endringsbrev med flere perioder med ingen reduksjon i midten")
    @Test
    void melder_inntekt_for_flere_mnd_med_0_inntekt_i_midten() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = EndringInntektScenarioer.endringMedInntektPå10k_utenom_mnd_2(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsprogramytelsen din " +
                "Du får 8 326 kroner i ungdomsprogramytelse for perioden fra 1. januar 2025 til 31. januar 2025. " +
                "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
                "Derfor har vi redusert ungdomsprogramytelsen din med et beløp som tilsvarer 66 prosent av inntekten din. " +
                "Du får 7 035 kroner i ungdomsprogramytelse for perioden fra 1. mars 2025 til 31. mars 2025. " +
                "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
                "Derfor har vi redusert ungdomsprogramytelsen din med et beløp som tilsvarer 66 prosent av inntekten din. " +
                "Pengene får du utbetalt innen fire dager. " +
                standardTekstEndringInntekt());

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelsen din</h1>"
            );
    }

    @DisplayName("Endringsbrev med 0 kr utbetaling pga for mye inntekt")
    @Test
    void melder_inntekt_0_utbetaling() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = EndringInntektScenarioer.endringMedInntekt0krUtbetaling(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsprogramytelsen din " +
                "Du får ikke utbetalt ungdomsprogramytelse for perioden fra 1. januar 2025 til 31. januar 2025. " +
                "Det er fordi du hadde en inntekt på 23 000 kroner i denne perioden. " +
                standardTekstIngenUtbetalingEndringInntekt());

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelsen din</h1>",
                "<h2>Hvorfor får du ingen utbetaling?</h2>"
            );
    }



    @DisplayName("Endringsbrev med flere perioder der det ikke er utbetaling i midten")
    @Test
    void melder_inntekt_for_flere_mnd_med_0_utbetaling_i_midten() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = EndringInntektScenarioer.endringMedInntektAlleKombinasjoner(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsprogramytelsen din " +
            "Du får 6 380 kroner i ungdomsprogramytelse for perioden fra 1. februar 2025 til 28. februar 2025. " +
            "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
            "Derfor har vi redusert ungdomsprogramytelsen din med et beløp som tilsvarer 66 prosent av inntekten din. " +
            "Du får 8 382 kroner i ungdomsprogramytelse for perioden fra 1. mai 2025 til 31. mai 2025. " +
            "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
            "Derfor har vi redusert ungdomsprogramytelsen din med et beløp som tilsvarer 66 prosent av inntekten din. " +
            "Pengene får du utbetalt innen fire dager. " +
            "Du får ikke utbetalt ungdomsprogramytelse for perioden fra 1. januar 2025 til 31. januar 2025. " +
            "Det er fordi du hadde en inntekt på 23 000 kroner i denne perioden. " +
            "Du får ikke utbetalt ungdomsprogramytelse for perioden fra 1. april 2025 til 30. april 2025. " +
            "Det er fordi du hadde en inntekt på 23 000 kroner i denne perioden. " +
            standardTekstIngenUtbetalingEndringInntekt());

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelsen din</h1>",
                "<h2>Hvorfor får du ingen utbetaling?</h2>"

            );
    }

    private String standardTekstIngenUtbetalingEndringInntekt() {
        return """
            Hvorfor får du ingen utbetaling? \
            Når du har en inntekt, får du mindre penger i ungdomsprogramytelse. \
            Vi regnet ut hva 66 prosent av inntekten din var for denne perioden, og så trakk vi dette beløpet fra pengene du får i ungdomsprogramytelsen. \
            Siden 66 prosent av inntekten din var høyere enn det du ville fått utbetalt i ungdomsprogramytelse for denne perioden, får du ingen utbetaling. \
            """ + standardTekstEndringInntekt();
    }

    private static String standardTekstEndringInntekt() {
        return "Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 11. ";
    }


    private Behandling lagScenario(UngTestScenario ungTestscenario) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        UngTestRepositories repositories = ungTestRepositories;
        var behandling = scenarioBuilder.buildOgLagreMedUng(repositories);

        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();


        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        UngTestScenario ungTestscenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));
        return lagScenario(ungTestscenario);
    }
}


