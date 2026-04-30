package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer.AvslagScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class FørstegangsAvslagTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final String FRITEKST_BOSTED = "Du har oppgitt adresse i et land som ikke er dekket av trygdeavtalen.";
    private static final String FRITEKST_BISTAND = "Du har ikke fått et vedtak fra NAV som sier at du har behov for bistand.";

    FørstegangsAvslagTest() {
        super(1, "Vi har avslått din søknad om aktivitetspenger");
    }

    @DisplayName("Avslag pga bostedsvilkåret - ytelseIkkeTilgjengeligPåBosted med fritekstBrev")
    @Test
    void avslagBosted() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttBosted(fom);

        var behandling = lagAvslagScenario(scenario, FRITEKST_BOSTED);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "Du har ikke bodd i et EØS-land",
                FRITEKST_BOSTED
            );
    }

    @DisplayName("Avslag pga bistandsvilkåret - harIkke14aVedtak med fritekstBrev")
    @Test
    void avslagBistand() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttBistand(fom);

        var behandling = lagAvslagScenario(scenario, FRITEKST_BISTAND);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "Du har ikke et oppfølgingsvedtak etter NAV-loven",
                FRITEKST_BISTAND
            );
    }

    private Behandling lagAvslagScenario(AvslagScenario avslagScenario, String fritekstBrev) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(avslagScenario.testScenario())
            .leggTilVilkår(avslagScenario.vilkårType(), Utfall.IKKE_OPPFYLT, avslagScenario.vilkårPeriode(), avslagScenario.avslagsårsak(), fritekstBrev);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        behandling.avsluttBehandling();
        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttBosted(fom);
        return lagAvslagScenario(scenario, FRITEKST_BOSTED);
    }
}
