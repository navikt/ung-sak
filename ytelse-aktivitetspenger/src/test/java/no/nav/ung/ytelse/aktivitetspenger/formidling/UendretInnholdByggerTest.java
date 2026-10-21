package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerRevurderingTestOppsett;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerUendretScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.BostedsAvklaringTestData;
import no.nav.ung.ytelse.aktivitetspenger.testdata.VilkårsavklaringTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.BrevTestUtils.brevDatoString;
import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class UendretInnholdByggerTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);
    private static final LocalDate MAKSDATO = FOM.plusWeeks(52).minusDays(1);
    private static final String FRITEKST = "Du får likevel aktivitetspenger fordi vilkåret fortsatt er oppfylt.";

    UendretInnholdByggerTest() {
        super(1, "Nav har ikke endret aktivitetspengene dine");
    }

    @DisplayName("Uendret vedtak etter at bruker ble varslet om mulig opphør pga bosted, men vilkåret er fortsatt oppfylt")
    @Test
    void uendretEtterVarsletOpphør() {
        var scenario = AktivitetspengerUendretScenarioer.uendretScenario(
            FOM,
            BostedsAvklaringTestData.opphør(vurdertPeriode(FOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            FRITEKST
        );

        var generertBrev = genererVedtaksbrev(lagUendretScenario(scenario).getId());

        assertUendretBrev(generertBrev, MAKSDATO);
        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextContains(VedtaksbrevVerifikasjon.HILSEN_NAV_KONTOR);
    }

    @DisplayName("Uendret vedtak etter at bruker ble varslet om mulig avslag pga bosted, men vilkåret er fortsatt oppfylt")
    @Test
    void uendretEtterVarsletAvslag() {
        var scenario = AktivitetspengerUendretScenarioer.uendretScenario(
            FOM,
            BostedsAvklaringTestData.avslag(vurdertPeriode(FOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            FRITEKST
        );

        assertUendretBrev(genererVedtaksbrev(lagUendretScenario(scenario).getId()), MAKSDATO);
    }

    @DisplayName("Uendret vedtak etter at bruker ble varslet om mulig opphør pga andre livsoppholdsytelser")
    @Test
    void uendretEtterVarsletOpphørLivsopphold() {
        var scenario = AktivitetspengerUendretScenarioer.uendretLivsoppholdScenario(
            FOM,
            VilkårsavklaringTestData.opphør(vurdertPeriode(FOM),
                AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER,
                AndreLivsoppholdsytelserAvklaringKildeType.NAV),
            FRITEKST
        );

        assertUendretBrev(genererVedtaksbrev(lagUendretScenario(scenario).getId()), MAKSDATO);
    }

    @DisplayName("Uendret vedtak etter at bruker ble varslet om mulig opphør pga bistandsvilkåret")
    @Test
    void uendretEtterVarsletOpphørBistand() {
        var scenario = AktivitetspengerUendretScenarioer.uendretBistandScenario(
            FOM,
            VilkårsavklaringTestData.opphør(vurdertPeriode(FOM),
                BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID,
                BistandsavklaringKildeType.BRUKER),
            FRITEKST
        );

        assertUendretBrev(genererVedtaksbrev(lagUendretScenario(scenario).getId()), MAKSDATO);
    }

    @DisplayName("Uendret vedtak krever ikke fritekst fra saksbehandler")
    @Test
    void uendretUtenFritekst() {
        var scenario = AktivitetspengerUendretScenarioer.uendretScenario(
            FOM,
            BostedsAvklaringTestData.opphør(vurdertPeriode(FOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            null
        );

        assertUendretBrev(genererVedtaksbrev(lagUendretScenario(scenario).getId()), MAKSDATO);
    }

    @DisplayName("Maksdato er siste dag før en avkortet periode")
    @Test
    void uendretMedAvkortetHale() {
        var avkortetFom = FOM.plusMonths(6);
        var scenario = AktivitetspengerUendretScenarioer.uendretBistandMedAvkortetHale(
            FOM,
            VilkårsavklaringTestData.opphør(vurdertPeriode(FOM),
                BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID,
                BistandsavklaringKildeType.BRUKER),
            avkortetFom
        );

        assertUendretBrev(genererVedtaksbrev(lagUendretScenario(scenario).getId()), avkortetFom.minusDays(1));
    }

    @DisplayName("Maksdato er slutten av den innvilgede perioden, også når den er etter perioden til vurdering")
    @Test
    void uendretMedKortVurderingsperiode() {
        var scenario = AktivitetspengerUendretScenarioer.uendretBistandMedKortVurderingsperiode(
            FOM,
            VilkårsavklaringTestData.opphør(kortVurdertPeriode(FOM),
                BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID,
                BistandsavklaringKildeType.BRUKER)
        );

        assertUendretBrev(genererVedtaksbrev(lagUendretScenario(scenario).getId()), MAKSDATO);
    }

    @DisplayName("Maksdato er siste dag før en avkortet periode som er etter perioden til vurdering")
    @Test
    void uendretMedKortVurderingsperiodeOgAvkortetHale() {
        var avkortetFom = FOM.plusMonths(9);
        var scenario = AktivitetspengerUendretScenarioer.uendretBistandMedKortVurderingsperiodeOgAvkortetHale(
            FOM,
            VilkårsavklaringTestData.opphør(kortVurdertPeriode(FOM),
                BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID,
                BistandsavklaringKildeType.BRUKER),
            avkortetFom
        );

        assertUendretBrev(genererVedtaksbrev(lagUendretScenario(scenario).getId()), avkortetFom.minusDays(1));
    }

    private static void assertUendretBrev(GenerertBrev generertBrev, LocalDate maksdato) {
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_UENDRET);
        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har ikke endret aktivitetspengene dine</h1>",
                "<p>Vi har vurdert saken din, og du får fortsatt aktivitetspenger så lenge du oppfyller vilkårene, men ikke lenger enn til "
                    + brevDatoString(maksdato) + ".</p>",
                "<span class=\"ikke-bryt\">§§ 2–8</span>.</p>",
                "<h2>Du må melde fra om endringer</h2>"
            )
            .asPlainTextNotContains(FRITEKST)
            .asPlainTextNotContains("Hvorfor får du aktivitetspenger?")
            .asPlainTextNotContains("arbeidsinntekt");
    }

    // Uendret-brevet krever en originalbehandling å sammenligne resultatet mot, så scenarioet bygges som en revurdering.
    private Behandling lagUendretScenario(AktivitetspengerTestScenario scenario) {
        return AktivitetspengerRevurderingTestOppsett.lagRevurdering(repositories, scenario, scenario, BehandlingDel.NAV_KONTOR);
    }

    private static Periode vurdertPeriode(LocalDate fom) {
        var tom = fom.plusWeeks(52).minusDays(1);
        return new Periode(fom.plusMonths(3), tom);
    }

    private static Periode kortVurdertPeriode(LocalDate fom) {
        return new Periode(fom.plusMonths(3), fom.plusMonths(6).minusDays(1));
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        var scenario = AktivitetspengerUendretScenarioer.uendretScenario(
            FOM,
            BostedsAvklaringTestData.opphør(vurdertPeriode(FOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            FRITEKST
        );
        return lagUendretScenario(scenario);
    }
}
