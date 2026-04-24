package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerEndringInntektScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringInntektTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);

    EndringInntektTest() {
        super(1, "Vi har endret aktivitetspengene dine");
    }

    @DisplayName("Endringsbrev med periode, innrapportert inntekt, reduksjon og utbetaling")
    @Test
    void standard_endring_inntekt() {
        var behandling = lagScenario(AktivitetspengerEndringInntektScenarioer.endringMedInntektPå10k(FOM));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret aktivitetspengene dine</h1>"
            )
            .asPlainTextContains("66 prosent av inntekten din")
            .asPlainTextContains("10 000 kroner");
    }

    @DisplayName("Endringsbrev med reduksjon for flere perioder")
    @Test
    void inntekt_reduksjon_flere_mnd() {
        var behandling = lagScenario(AktivitetspengerEndringInntektScenarioer.endringMedInntektPå10kFlereMnd(FOM));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret aktivitetspengene dine</h1>"
            )
            .asPlainTextContains("Pengene får du utbetalt innen fire dager");
    }

    @DisplayName("Endringsbrev med ingen utbetaling pga for mye inntekt")
    @Test
    void inntekt_ingen_utbetaling() {
        var behandling = lagScenario(AktivitetspengerEndringInntektScenarioer.endringMedInntektIngenUtbetaling(FOM));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret aktivitetspengene dine</h1>",
                "<h2>Hvorfor får du ingen utbetaling?</h2>"
            )
            .asPlainTextContains("23 000 kroner");
    }

    @DisplayName("Endringsbrev med reduksjon og ingen utbetaling")
    @Test
    void inntekt_reduksjon_og_ingen_utbetaling() {
        var behandling = lagScenario(AktivitetspengerEndringInntektScenarioer.endringInntektRedusertOgIngenUtbetaling(FOM));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret aktivitetspengene dine</h1>",
                "<h2>Hvorfor får du ingen utbetaling?</h2>"
            )
            .asPlainTextContains("10 000 kroner")
            .asPlainTextContains("23 000 kroner");
    }

    private Behandling lagScenario(AktivitetspengerTestScenario testScenario) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(testScenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        return lagScenario(AktivitetspengerEndringInntektScenarioer.endringMedInntektPå10k(FOM));
    }
}

