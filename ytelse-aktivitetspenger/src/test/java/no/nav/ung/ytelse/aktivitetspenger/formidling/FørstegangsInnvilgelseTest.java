package no.nav.ung.ytelse.aktivitetspenger.formidling;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenario;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerBeregningsgrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.BrevScenarioerUtils;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class FørstegangsInnvilgelseTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    @Inject
    private AktivitetspengerBeregningsgrunnlagRepository beregningsgrunnlagRepository;

    FørstegangsInnvilgelseTest() {
        super(2, "Du får aktivitetspenger");
    }

    @DisplayName("Innvilgelse med lav sats og lav inntekt (minsteytelse)")
    @Test
    void lavSatsMinsteytelse() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var scenario = FørstegangsbehandlingScenarioer.innvilget19årLavInntekt(fom);
        var satsGrunnlag = BrevScenarioerUtils.lavSatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(fom);

        var behandling = lagScenarioMedBeregning(scenario, satsGrunnlag, beregningsgrunnlag, fom);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextContains("Du får aktivitetspenger")
            .asPlainTextContains("Fordi du er under 25 år")
            .containsHtmlSubSequenceOnce(
                "<h1>Du får aktivitetspenger</h1>",
                "<h2>Hvorfor får du penger?</h2>"
            );
    }

    @DisplayName("Innvilgelse med lav sats og høy inntekt (beregningsgrunnlag)")
    @Test
    void lavSatsHøyInntekt() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var scenario = FørstegangsbehandlingScenarioer.innvilget19årHøyInntekt(fom);
        var satsGrunnlag = BrevScenarioerUtils.lavSatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedHøyInntekt(fom);

        var behandling = lagScenarioMedBeregning(scenario, satsGrunnlag, beregningsgrunnlag, fom);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextContains("Du får aktivitetspenger")
            .asPlainTextContains("66 prosent")
            .containsHtmlSubSequenceOnce(
                "<h1>Du får aktivitetspenger</h1>",
                "<h2>Hvorfor får du penger?</h2>"
            );
    }

    @DisplayName("Innvilgelse med høy sats og lav inntekt (minsteytelse)")
    @Test
    void høySatsMinsteytelse() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var scenario = FørstegangsbehandlingScenarioer.innvilget27årHøySats(fom);
        var satsGrunnlag = BrevScenarioerUtils.høySatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(fom);

        var behandling = lagScenarioMedBeregning(scenario, satsGrunnlag, beregningsgrunnlag, fom);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextContains("Du får aktivitetspenger")
            .asPlainTextContains("Fordi du er over 25 år")
            .containsHtmlSubSequenceOnce(
                "<h1>Du får aktivitetspenger</h1>",
                "<h2>Hvorfor får du penger?</h2>"
            );
    }

    @DisplayName("Innvilgelse med høy sats og høy inntekt (beregningsgrunnlag)")
    @Test
    void høySatsHøyInntekt() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var scenario = FørstegangsbehandlingScenarioer.innvilget27årHøySatsHøyInntekt(fom);
        var satsGrunnlag = BrevScenarioerUtils.høySatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedHøyInntekt(fom);

        var behandling = lagScenarioMedBeregning(scenario, satsGrunnlag, beregningsgrunnlag, fom);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextContains("Du får aktivitetspenger")
            .asPlainTextContains("66 prosent")
            .containsHtmlSubSequenceOnce(
                "<h1>Du får aktivitetspenger</h1>",
                "<h2>Hvorfor får du penger?</h2>"
            );
    }

    @DisplayName("Innvilgelse med barnetillegg")
    @Test
    void medBarnetillegg() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var scenario = FørstegangsbehandlingScenarioer.innvilget19årMedBarn15DagerEtterStartdato(fom);
        var satsGrunnlag = BrevScenarioerUtils.lavSatsGrunnlagMedBarn(fom, 1);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(fom);

        var behandling = lagScenarioMedBeregning(scenario, satsGrunnlag, beregningsgrunnlag, fom);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextContains("Du får aktivitetspenger")
            .asPlainTextContains("barnetillegg")
            .containsHtmlSubSequenceOnce(
                "<h1>Du får aktivitetspenger</h1>"
            );
    }

    @DisplayName("Delvis innvilget - 18 år som blir 19 år i program")
    @Test
    void delvisInnvilget() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        LocalDate nittenårsdag = fom.plusDays(10);
        var scenario = FørstegangsbehandlingScenarioer.innvilgetDelvis(fom, nittenårsdag);
        var satsGrunnlag = BrevScenarioerUtils.lavSatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(nittenårsdag);

        var behandling = lagScenarioMedBeregning(scenario, satsGrunnlag, beregningsgrunnlag, nittenårsdag);

        GenerertBrev generertBrev = genererVedtaksbrevUtenLagring(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextContains("Du får aktivitetspenger")
            .asPlainTextContains(BrevTestUtils.brevDatoString(nittenårsdag));
    }

    private Behandling lagScenarioMedBeregning(AktivitetspengerTestScenario scenario,
                                                AktivitetspengerSatsGrunnlag satsGrunnlag,
                                                Beregningsgrunnlag beregningsgrunnlag,
                                                LocalDate skjæringstidspunkt) {
        var behandling = lagScenario(scenario);

        beregningsgrunnlagRepository.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag);

        var satsTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(skjæringstidspunkt, skjæringstidspunkt.plusYears(1), satsGrunnlag)
        ));
        beregningsgrunnlagRepository.lagre(behandling.getId(),
            new AktivitetspengerSatsResultat(satsTidslinje, "regelInput", "regelSporing"));

        return behandling;
    }

    private Behandling lagScenario(AktivitetspengerTestScenario scenario) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(scenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(testRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        var scenario = FørstegangsbehandlingScenarioer.innvilget19årLavInntekt(LocalDate.of(2024, 12, 1));
        var satsGrunnlag = BrevScenarioerUtils.lavSatsGrunnlag(LocalDate.of(2024, 12, 1));
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(LocalDate.of(2024, 12, 1));
        return lagScenarioMedBeregning(scenario, satsGrunnlag, beregningsgrunnlag, LocalDate.of(2024, 12, 1));
    }
}
