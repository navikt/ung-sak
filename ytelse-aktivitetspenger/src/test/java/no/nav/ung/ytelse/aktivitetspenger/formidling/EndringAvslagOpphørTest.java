package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerEndringAvslagScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerEndringAvslagScenarioer.EndringAvslagScenario;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerOpphørScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerOpphørScenarioer.OpphørScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.BrevTestUtils.brevDatoString;
import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringAvslagOpphørTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);

    EndringAvslagOpphørTest() {
        super(1, "Du får ikke lenger aktivitetspenger");
    }

    @DisplayName("Opphør pga bostedsvilkåret - ytelseIkkeTilgjengeligPåBosted")
    @Test
    void opphørBosted() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBosted(FOM);
        var behandling = lagOpphørScenario(scenario, null);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(scenario.opphørtVilkårPeriode().getFom()) + " får du ikke lenger aktivitetspenger",
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune"
            );
    }

    @DisplayName("Opphør pga bostedsvilkåret - ytelseIkkeTilgjengeligPåFolkeregistrertEllerBostedsadresse")
    @Test
    void opphørBostedFolkeregistrert() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBostedFolkeregistrert(FOM);
        var behandling = lagOpphørScenario(scenario, null);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(scenario.opphørtVilkårPeriode().getFom()) + " får du ikke lenger aktivitetspenger",
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune"
            );
    }

    @DisplayName("Opphør pga bostedsvilkåret - ytelseIkkePåArbeidsstedStudiested")
    @Test
    void opphørArbeidsstedStudiested() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaArbeidsstedStudiested(FOM);
        var behandling = lagOpphørScenario(scenario, null);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(scenario.opphørtVilkårPeriode().getFom()) + " får du ikke lenger aktivitetspenger",
                "studere eller jobbe i Trondheim kommune"
            );
    }

    @DisplayName("Opphør med fritekst på bostedsvilkåret")
    @Test
    void opphørBostedFritekst() {
        var fritekst = "Du har flyttet til et sted utenfor Trondheim og har derfor ikke lenger rett.";
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBostedAnnet(FOM, fritekst);
        var behandling = lagOpphørScenario(scenario, fritekst);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(scenario.opphørtVilkårPeriode().getFom()) + " får du ikke lenger aktivitetspenger",
                fritekst
            );
    }

    @DisplayName("Endring/avslag pga bostedsvilkåret - ytelseIkkeTilgjengeligPåBosted")
    @Test
    void endringAvslagBosted() {
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaBosted(FOM);
        var behandling = lagEndringAvslagScenario(scenario, null);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(scenario.avslåttVilkårPeriode().getFom()) + " til "
                    + brevDatoString(scenario.avslåttVilkårPeriode().getTom()),
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune"
            );
    }

    @DisplayName("Endring/avslag pga bostedsvilkåret - ytelseIkkeTilgjengeligPåFolkeregistrertEllerBostedsadresse")
    @Test
    void endringAvslagBostedFolkeregistrert() {
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaBostedFolkeregistrert(FOM);
        var behandling = lagEndringAvslagScenario(scenario, null);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(scenario.avslåttVilkårPeriode().getFom()) + " til "
                    + brevDatoString(scenario.avslåttVilkårPeriode().getTom()),
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune"
            );
    }

    @DisplayName("Endring/avslag pga bostedsvilkåret - ytelseIkkePåArbeidsstedStudiested")
    @Test
    void endringAvslagArbeidsstedStudiested() {
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaArbeidsstedStudiested(FOM);
        var behandling = lagEndringAvslagScenario(scenario, null);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(scenario.avslåttVilkårPeriode().getFom()) + " til "
                    + brevDatoString(scenario.avslåttVilkårPeriode().getTom()),
                "studere eller jobbe i Trondheim kommune"
            );
    }

    @DisplayName("Endring/avslag med fritekst på bostedsvilkåret")
    @Test
    void endringAvslagBostedFritekst() {
        var fritekst = "Du har midlertidig ikke bostedsadresse i Trondheim og har derfor ikke rett i perioden.";
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaBostedAnnet(FOM, fritekst);
        var behandling = lagEndringAvslagScenario(scenario, fritekst);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(scenario.avslåttVilkårPeriode().getFom()) + " til "
                    + brevDatoString(scenario.avslåttVilkårPeriode().getTom()),
                fritekst
            );
    }

    private Behandling lagOpphørScenario(OpphørScenario scenario, String fritekstBrev) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(scenario.opphørScenario())
            .leggTilVilkår(scenario.vilkårType(), Utfall.IKKE_OPPFYLT, scenario.opphørtVilkårPeriode(), scenario.avslagsårsak(), fritekstBrev)
            .leggTilBostedsAvklaring(scenario.bostedsAvklaring());

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    private Behandling lagEndringAvslagScenario(EndringAvslagScenario scenario, String fritekstBrev) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(scenario.endringAvslagScenario())
            .leggTilVilkår(scenario.vilkårType(), Utfall.IKKE_OPPFYLT, scenario.avslåttVilkårPeriode(), scenario.avslagsårsak(), fritekstBrev)
            .leggTilBostedsAvklaring(scenario.bostedsAvklaring());

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBosted(FOM);
        return lagOpphørScenario(scenario, null);
    }
}

