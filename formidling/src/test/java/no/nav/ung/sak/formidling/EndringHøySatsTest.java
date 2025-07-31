package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringHøySatsInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.EndringHøySatsStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringHøySatsTest extends AbstractVedtaksbrevInnholdByggerTest {

    EndringHøySatsTest() {
        super(1, "Du får mer i ungdomsprogramytelse fordi du fyller 25 år");
    }


    @DisplayName("Endringsbrev for overgang til høy sats")
    @Test
    void standardEndringHøySats() {
        LocalDate fødselsdato = LocalDate.of(2000, 3, 25);
        var ungTestGrunnlag = BrevScenarioer.endring25År(fødselsdato);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du fyller 25 år \
                Du får mer penger fordi du fyller 25 år 25. mars 2025. \
                Fra og med denne datoen får du 974 kroner per dag, utenom lørdag og søndag. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 9. \
                """);


        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_HØY_SATS);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i ungdomsprogramytelse fordi du fyller 25 år</h1>"
            );

    }

    @Test
    void medBarnetillegg() {
        LocalDate fødselsdato = LocalDate.of(2000, 3, 25);
        var ungTestGrunnlag = BrevScenarioer.endring25ÅrMedToBarn(fødselsdato);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du fyller 25 år \
                Du får mer penger fordi du fyller 25 år 25. mars 2025. \
                Fra og med denne datoen får du 1 048 kroner per dag, utenom lørdag og søndag. \
                Denne summen inkluderer også et barnetillegg på 74 kroner per dag fordi du har barn. \
                Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 8 jf. § 9. \
                """);


        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_HØY_SATS);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i ungdomsprogramytelse fordi du fyller 25 år</h1>"
            );

    }

    private Behandling lagScenario(UngTestScenario ungTestscenario) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);


        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();


        return behandling;
    }


    @Override
    protected List<VedtaksbrevInnholdbyggerStrategy> lagVedtaksbrevByggerStrategier() {
        return List.of(new EndringHøySatsStrategy(
            new EndringHøySatsInnholdBygger(ungTestRepositories.ungdomsytelseGrunnlagRepository()))
        );
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        UngTestScenario ungTestscenario = BrevScenarioer.endring25År(LocalDate.of(1999, 3, 25));
        return lagScenario(ungTestscenario);
    }
}


