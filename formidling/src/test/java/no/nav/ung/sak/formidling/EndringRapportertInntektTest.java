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
                "Det er fordi du har hatt en inntekt på 10 000 kroner i denne perioden. " +
                standardTekstEndringInntekt());

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelsen din</h1>",
                "<a title=\"utregningseksempler\" href=\"https://nav.no/ungdomsportal/beregning\">Se eksempel på hvordan vi regner ut ungdomsprogramytelsen basert på inntekt i Ungdomsportalen.</a>"
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
                "Det er fordi du har hatt en inntekt på 10 000 kroner i denne perioden. " +
                "Du får 6 380 kroner i ungdomsprogramytelse for perioden fra 1. februar 2025 til 28. februar 2025. " +
                "Det er fordi du har hatt en inntekt på 10 000 kroner i denne perioden. " +
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
                "Det er fordi du har hatt en inntekt på 10 000 kroner i denne perioden. " +
                "Du får 7 035 kroner i ungdomsprogramytelse for perioden fra 1. mars 2025 til 31. mars 2025. " +
                "Det er fordi du har hatt en inntekt på 10 000 kroner i denne perioden. " +
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
                "Du får 0 kroner i ungdomsprogramytelse for perioden fra 1. januar 2025 til 31. januar 2025. " +
                "Det er fordi du har hatt en inntekt på 23 000 kroner i denne perioden. " +
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

    @DisplayName("Endringsbrev med flere perioder der det ikke er utbetaling i midten")
    @Test
    void melder_inntekt_for_flere_mnd_med_0_utbetaling_i_midten() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = EndringInntektScenarioer.endringMedInntektReduksjonOgIngenUtbetalingKombinasjon(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsprogramytelsen din " +
                "Du får 8 326 kroner i ungdomsprogramytelse for perioden fra 1. januar 2025 til 31. januar 2025. " +
                "Det er fordi du har hatt en inntekt på 10 000 kroner i denne perioden. " +
                "Du får 0 kroner i ungdomsprogramytelse for perioden fra 1. februar 2025 til 28. februar 2025. " +
                "Det er fordi du har hatt en inntekt på 23 000 kroner i denne perioden. " +
                "Du får 7 035 kroner i ungdomsprogramytelse for perioden fra 1. mars 2025 til 31. mars 2025. " +
                "Det er fordi du har hatt en inntekt på 10 000 kroner i denne perioden. " +
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

    @NotNull
    private static String standardTekstEndringInntekt() {
        return """
            Pengene får du utbetalt før den 10. denne måneden. \
            Når du har en inntekt, får du mindre penger i ungdomsprogramytelse. \
            Vi regner ut hva 66 prosent av inntekten din er hver måned, og så trekker vi dette beløpet fra pengene du får i ungdomsprogramytelsen for den måneden. \
            Likevel får du til sammen mer penger når du både har en inntekt og får ungdomsprogramytelse, enn hvis du bare hadde fått penger gjennom ungdomsprogramytelsen. \
            Se eksempel på hvordan vi regner ut ungdomsprogramytelsen basert på inntekt i Ungdomsportalen. \
            Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 11. \
            """;
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


