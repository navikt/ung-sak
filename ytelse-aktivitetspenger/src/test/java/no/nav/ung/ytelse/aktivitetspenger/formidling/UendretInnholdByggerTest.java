package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerUendretScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.ytelse.aktivitetspenger.testdata.BostedsAvklaringTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.BrevTestUtils.brevDatoString;
import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class UendretInnholdByggerTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);

    UendretInnholdByggerTest() {
        super(1, "Nav har ikke endret aktivitetspengene dine");
    }

    @DisplayName("Uendret vedtak etter at bruker ble varslet om mulig opphør pga bosted, men vilkåret er fortsatt oppfylt")
    @Test
    void uendretEtterVarsletOpphør() {
        var scenario = AktivitetspengerUendretScenarioer.uendretScenario(
            FOM,
            BostedsAvklaringTestData.opphør(vurdertPeriode(FOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            null
        );
        var behandling = lagUendretScenario(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_UENDRET);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har ikke endret aktivitetspengene dine</h1>",
                "Du vil fortsatt få aktivitetspenger fra " + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getFom())
            );
    }

    @DisplayName("Uendret vedtak etter at bruker ble varslet om mulig avslag pga bosted, men vilkåret er fortsatt oppfylt")
    @Test
    void uendretEtterVarsletAvslag() {
        var scenario = AktivitetspengerUendretScenarioer.uendretScenario(
            FOM,
            BostedsAvklaringTestData.avslag(vurdertPeriode(FOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            null
        );
        var behandling = lagUendretScenario(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_UENDRET);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har ikke endret aktivitetspengene dine</h1>",
                "Du får likevel aktivitetspenger i perioden fra og med " + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getFom())
            );
    }

    @DisplayName("Uendret vedtak med fritekst på bostedsvurderingen")
    @Test
    void uendretMedFritekst() {
        var fritekst = "Fritekstbegrunnelse som beskriver hvorfor du likevel beholder ytelsen";
        var scenario = AktivitetspengerUendretScenarioer.uendretScenario(
            FOM,
            BostedsAvklaringTestData.opphør(vurdertPeriode(FOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            fritekst
        );
        var behandling = lagUendretScenario(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_UENDRET);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har ikke endret aktivitetspengene dine</h1>",
                fritekst
            );
    }

    private Behandling lagUendretScenario(AktivitetspengerTestScenario scenario) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(scenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    private static Periode vurdertPeriode(LocalDate fom) {
        var tom = fom.plusWeeks(52).minusDays(1);
        return new Periode(fom.plusMonths(3), tom);
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        var scenario = AktivitetspengerUendretScenarioer.uendretScenario(
            FOM,
            BostedsAvklaringTestData.opphør(vurdertPeriode(FOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            null
        );
        return lagUendretScenario(scenario);
    }
}
