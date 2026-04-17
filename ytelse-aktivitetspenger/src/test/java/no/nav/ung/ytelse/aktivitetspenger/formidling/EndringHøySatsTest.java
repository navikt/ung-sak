package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerEndringHøySatsScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndringHøySatsTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);

    EndringHøySatsTest() {
        super(1, "Du får mer i aktivitetspenger");
    }

    @DisplayName("Overgang fra lav til høy sats sender brev")
    @Test
    void lavTilHøySats() {
        var behandling = lagScenario(AktivitetspengerEndringHøySatsScenarioer.lavTilHøySats(FOM));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_HØY_SATS);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i aktivitetspenger fordi du fyller 25 år \
                Du får mer penger fordi du fyller 25 år 16. august 2025. \
                Fra og med denne datoen får du 1 022 kroner per dag, utenom lørdag og søndag (før skatt). \
                """);

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i aktivitetspenger fordi du fyller 25 år</h1>"
            );
    }

    @DisplayName("Overgang fra beregningsgrunnlag til høy sats sender brev")
    @Test
    void beregningsgrunnlagTilHøySats() {
        var behandling = lagScenario(AktivitetspengerEndringHøySatsScenarioer.beregningsgrunnlagTilHøySats(FOM));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_HØY_SATS);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i aktivitetspenger fordi du fyller 25 år \
                Du får mer penger fordi du fyller 25 år 16. august 2025. \
                Fra og med denne datoen får du 1 022 kroner per dag, utenom lørdag og søndag (før skatt). \
                """);

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i aktivitetspenger fordi du fyller 25 år</h1>"
            );
    }

    @DisplayName("Alle perioder med beregningsgrunnlag sender ikke brev")
    @Test
    void allePerioderBeregningsgrunnlagGirIngenBrev() {
        var behandling = lagScenario(AktivitetspengerEndringHøySatsScenarioer.beregningsgrunnlagHøyereEnnSatsForAllePerioder(FOM));

        assertThatThrownBy(() -> genererVedtaksbrev(behandling.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ingen vedtaksbrev");
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
        return lagScenario(AktivitetspengerEndringHøySatsScenarioer.lavTilHøySats(FOM));
    }
}
