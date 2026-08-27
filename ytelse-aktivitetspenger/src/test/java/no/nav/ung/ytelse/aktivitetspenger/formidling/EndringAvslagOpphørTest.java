package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerOpphørScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerOpphørScenarioer.OpphørScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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

    @Override
    protected Behandling lagScenarioForFellesTester() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBosted(FOM);
        return lagOpphørScenario(scenario, null);
    }
}

