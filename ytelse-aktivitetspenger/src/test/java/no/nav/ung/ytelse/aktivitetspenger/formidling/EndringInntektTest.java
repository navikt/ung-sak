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
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret aktivitetspengene dine " +
                "Du får 8 382 kroner i aktivitetspenger for perioden fra 1. september 2025 til 30. september 2025. " +
                "Pengene får du utbetalt innen fire dager. " +
                "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
                "Derfor har vi redusert aktivitetspengene dine med et beløp som tilsvarer 66 prosent av inntekten din. ");

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_INNTEKT);

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret aktivitetspengene dine</h1>"
            );
    }

    @DisplayName("Endringsbrev med reduksjon for flere perioder")
    @Test
    void inntekt_reduksjon_flere_mnd() {
        var behandling = lagScenario(AktivitetspengerEndringInntektScenarioer.endringMedInntektPå10kFlereMnd(FOM));
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret aktivitetspengene dine " +
                "Du får 8 382 kroner i aktivitetspenger for perioden fra 1. september 2025 til 30. september 2025. " +
                "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
                "Derfor har vi redusert aktivitetspengene dine med et beløp som tilsvarer 66 prosent av inntekten din. " +
                "Du får 9 062 kroner i aktivitetspenger for perioden fra 1. oktober 2025 til 31. oktober 2025. " +
                "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
                "Derfor har vi redusert aktivitetspengene dine med et beløp som tilsvarer 66 prosent av inntekten din. " +
                "Pengene får du utbetalt innen fire dager. ");

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_INNTEKT);

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret aktivitetspengene dine</h1>"
            );
    }

    @DisplayName("Endringsbrev med ingen utbetaling pga for mye inntekt")
    @Test
    void inntekt_ingen_utbetaling() {
        var behandling = lagScenario(AktivitetspengerEndringInntektScenarioer.endringMedInntektIngenUtbetaling(FOM));
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret aktivitetspengene dine " +
                "Du får ikke utbetalt aktivitetspenger for perioden fra 1. september 2025 til 30. september 2025. " +
                "Det er fordi du hadde en inntekt på 23 000 kroner i denne perioden. " +
                standardTekstIngenUtbetalingEndringInntekt());

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_INNTEKT);

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret aktivitetspengene dine</h1>",
                "<h2>Hvorfor får du ingen utbetaling?</h2>"
            );
    }

    @DisplayName("Endringsbrev med reduksjon og ingen utbetaling")
    @Test
    void inntekt_reduksjon_og_ingen_utbetaling() {
        var behandling = lagScenario(AktivitetspengerEndringInntektScenarioer.endringInntektRedusertOgIngenUtbetaling(FOM));
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret aktivitetspengene dine " +
                "Du får 9 062 kroner i aktivitetspenger for perioden fra 1. oktober 2025 til 31. oktober 2025. " +
                "Pengene får du utbetalt innen fire dager. " +
                "Du får dette beløpet siden du hadde en inntekt på 10 000 kroner i denne perioden. " +
                "Du får ikke utbetalt aktivitetspenger for perioden fra 1. september 2025 til 30. september 2025. " +
                "Det er fordi du hadde en inntekt på 23 000 kroner i denne perioden. " +
                standardTekstIngenUtbetalingEndringInntekt());

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_INNTEKT);

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret aktivitetspengene dine</h1>",
                "<h2>Hvorfor får du ingen utbetaling?</h2>"
            );
    }

    private static String standardTekstIngenUtbetalingEndringInntekt() {
        return "Hvorfor får du ingen utbetaling? " +
            "Når du har en inntekt, får du mindre penger i aktivitetspenger. " +
            "Vi regnet ut hva 66 prosent av inntekten din var for denne perioden, og så trakk vi dette beløpet fra pengene du får i aktivitetspenger. " +
            "Siden 66 prosent av inntekten din var høyere enn det du ville fått utbetalt i aktivitetspenger for denne perioden, får du ingen utbetaling. ";
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

