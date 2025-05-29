package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class BrevGenerererTjenesteEndringInntektTest extends AbstractVedtaksbrevInnholdByggerTest {

    BrevGenerererTjenesteEndringInntektTest() {
        super(1, "Vi har endret ungdomsytelsen din");
    }

    @DisplayName("Endringsbrev med periode, innrapportert inntekt, reduksjon og utbetaling")
    @Test
    void standardEndringRapportertInntekt() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.endringMedInntektPå10k_19år(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsytelsen din " +
                "Du får 8 329 kroner i ungdomsytelse for perioden fra 1. januar 2025 til 31. januar 2025. " +
                "Det er fordi du har hatt en inntekt på 10 000 kroner i denne perioden. " +
                "Pengene får du utbetalt før den 10. denne måneden. " +
                "Når du har en inntekt, får du mindre penger i ungdomsytelse. " +
                "Vi regner ut hva 66 prosent av inntekten din er hver måned, og så trekker vi dette beløpet fra pengene du får i ungdomsytelsen for den måneden. " +
                "Likevel får du til sammen mer penger når du både har en inntekt og får ungdomsytelse, enn hvis du bare hadde fått penger gjennom ungdomsytelsen. " +
                "Se eksempel på hvordan vi regner ut ungdomsytelsen basert på inntekt i Ungdomsportalen. " +
                "Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. ");

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsytelsen din</h1>",
                "<a title=\"utregningseksempler\" href=\"https://nav.no/ungdomsportal/beregning\">Se eksempel på hvordan vi regner ut ungdomsytelsen basert på inntekt i Ungdomsportalen.</a>"
            );
    }

    @DisplayName("Endringsbrev med flere perioder")
    @Test
    void melder_inntekt_for_flere_mnd() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.endringMedInntektPå10k_flere_mnd_19år(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsytelsen din " +
                "Du får 14 710 kroner i ungdomsytelse for perioden fra 1. januar 2025 til 28. februar 2025. " +
                "Det er fordi du har hatt en inntekt på 20 000 kroner i denne perioden. " +
                "Pengene får du utbetalt før den 10. denne måneden. " +
                "Vi har registrert følgende inntekter på deg: " +
                "Fra 1. januar 2025 til 31. januar 2025 har du hatt en inntekt på 10 000 kroner. " +
                "Fra 1. februar 2025 til 28. februar 2025 har du hatt en inntekt på 10 000 kroner. " +
                "Når du har en inntekt, får du mindre penger i ungdomsytelse. " +
                "Vi regner ut hva 66 prosent av inntekten din er hver måned, og så trekker vi dette beløpet fra pengene du får i ungdomsytelsen for den måneden. " +
                "Likevel får du til sammen mer penger når du både har en inntekt og får ungdomsytelse, enn hvis du bare hadde fått penger gjennom ungdomsytelsen. " +
                "Se eksempel på hvordan vi regner ut ungdomsytelsen basert på inntekt i Ungdomsportalen. " +
                "Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. ");

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsytelsen din</h1>"
            );
    }

    @DisplayName("Ingen brev ved ingen rapportert inntekt og ingen inntekt")
    @Test
    void full_ungdomsprogram_med_ingen_rapportert_inntekt_gir_ingen_brev() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.endring0KrInntekt_19år(fom);
        var behandling = lagScenario(ungTestGrunnlag);
        assertThat(brevGenerererTjeneste.genererVedtaksbrevForBehandling(behandling.getId(), true)).isNull();

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
    protected VedtaksbrevInnholdBygger lagVedtaksbrevInnholdBygger() {
        return new EndringRapportertInntektInnholdBygger(ungTestRepositories.tilkjentYtelseRepository());
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        UngTestScenario ungTestscenario = BrevScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));
        return lagScenario(ungTestscenario);
    }
}


