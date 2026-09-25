package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerEndringAvslagScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerOpphørScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
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

    @DisplayName("Opphør pga bostedsvilkåret - YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED")
    @Test
    void opphørBosted() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBosted(FOM);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getFom()) + " får du ikke lenger aktivitetspenger",
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune"
            );
    }

    @DisplayName("Opphør pga bostedsvilkåret - YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED")
    @Test
    void opphørArbeidsstedStudiested() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaArbeidsstedStudiested(FOM);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getFom()) + " får du ikke lenger aktivitetspenger",
                "studere eller jobbe i Trondheim kommune"
            );
    }

    @DisplayName("Opphør med fritekst på bostedsvilkåret")
    @Test
    void opphørBostedFritekst() {
        var fritekst = "Du har flyttet til et sted utenfor Trondheim kommune og har derfor ikke lenger rett.";
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBostedAnnet(FOM, fritekst);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getFom()) + " får du ikke lenger aktivitetspenger",
                fritekst
            );
    }

    @DisplayName("Endring/avslag pga bostedsvilkåret - YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED")
    @Test
    void endringAvslagBosted() {
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaBosted(FOM);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getFom()) + " til "
                    + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getTom()),
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune"
            );
    }

    @DisplayName("Endring/avslag pga bostedsvilkåret - YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED")
    @Test
    void endringAvslagArbeidsstedStudiested() {
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaArbeidsstedStudiested(FOM);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getFom()) + " til "
                    + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getTom()),
                "studere eller jobbe i Trondheim kommune"
            );
    }

    @DisplayName("Endring/avslag med fritekst på bostedsvilkåret")
    @Test
    void endringAvslagBostedFritekst() {
        var fritekst = "Du har midlertidig ikke bostedsadresse i Trondheim kommune og har derfor ikke rett i perioden.";
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaBostedAnnet(FOM, fritekst);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getFom()) + " til "
                    + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getTom()),
                fritekst
            );
    }

    @DisplayName("Endring/avslag fra en periode før avkortet hale gir endrings-/avslagsbrev")
    @Test
    void endringAvslagBostedMedAvkortetHale() {
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaBostedMedAvkortetHale(FOM);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getFom()) + " til "
                    + brevDatoString(scenario.bostedsAvklaringer().getFirst().periode().getTom()),
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune"
            );
    }

    @DisplayName("Opphør pga andre livsoppholdsytelser - ytelsen navngis")
    @Test
    void opphørAndreLivsoppholdsytelser() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaAndreLivsoppholdsytelser(
            FOM, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER, null);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(livsoppholdsperiode(scenario).getFom()) + " får du ikke lenger aktivitetspenger",
                "Det er fordi du får dagpenger fra denne datoen. Du kan ikke få aktivitetspenger samtidig som du får "
                    + "en annen livsoppholdsytelse."
            );
    }

    @DisplayName("Opphør pga andre livsoppholdsytelser - MOTTAR_ANNEN_YTELSE navngir ytelsen i friteksten")
    @Test
    void opphørAndreLivsoppholdsytelserAnnenYtelse() {
        var fritekst = "Du får en ytelse fra en annen ordning som dekker livsoppholdet ditt.";
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaAndreLivsoppholdsytelser(
            FOM, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE, fritekst);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Det er fordi du får en annen livsoppholdsytelse fra denne datoen. Du kan ikke få aktivitetspenger samtidig.",
                fritekst
            );
    }

    @DisplayName("Opphør på både bosteds- og livsoppholdsvilkåret - innledning skrives én gang")
    @Test
    void opphørBostedOgAndreLivsoppholdsytelser() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBostedOgAndreLivsoppholdsytelser(FOM);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(livsoppholdsperiode(scenario).getFom()) + " får du ikke lenger aktivitetspenger",
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune",
                "Det er fordi du får dagpenger fra denne datoen"
            );
    }

    @DisplayName("Endring/avslag pga andre livsoppholdsytelser - ytelsen navngis")
    @Test
    void endringAvslagAndreLivsoppholdsytelser() {
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaAndreLivsoppholdsytelser(
            FOM, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_TILTAKSPENGER, null);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(livsoppholdsperiode(scenario).getFom()) + " til "
                    + brevDatoString(livsoppholdsperiode(scenario).getTom()),
                "Det er fordi du får tiltakspenger i denne perioden. Du kan ikke få aktivitetspenger samtidig som du får "
                    + "en annen livsoppholdsytelse."
            );
    }

    @DisplayName("Opphør pga bistandsvilkåret - mangler oppfølgingsvedtak etter § 14a")
    @Test
    void opphørBistand() {
        var fritekst = "Oppfølgingsvedtaket ditt etter § 14a er avsluttet.";
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBistand(FOM, fritekst);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ikke lenger aktivitetspenger</h1>",
                "Fra " + brevDatoString(bistandsperiode(scenario).getFom()) + " får du ikke lenger aktivitetspenger",
                "For å ha rett på aktivitetspenger må du ha et oppfølgingsvedtak etter NAV-loven § 14a. "
                    + "Fordi du ikke lenger har et slikt vedtak, får du ikke lenger aktivitetspenger.",
                fritekst
            );
    }

    @DisplayName("Endring/avslag pga bistandsvilkåret - mangler oppfølgingsvedtak etter § 14a")
    @Test
    void endringAvslagBistand() {
        var fritekst = "Du hadde ikke oppfølgingsvedtak etter § 14a i denne perioden.";
        var scenario = AktivitetspengerEndringAvslagScenarioer.avslagPgaBistand(FOM, fritekst);
        var behandling = lagBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Nav har endret aktivitetspengene dine</h1>",
                "Du får ikke aktivitetspenger i perioden fra "
                    + brevDatoString(bistandsperiode(scenario).getFom()) + " til "
                    + brevDatoString(bistandsperiode(scenario).getTom()),
                "For å ha rett på aktivitetspenger må du ha et oppfølgingsvedtak etter NAV-loven § 14a. "
                    + "Fordi du ikke har et slikt vedtak i denne perioden, får du ikke aktivitetspenger.",
                fritekst
            );
    }

    private static Periode bistandsperiode(AktivitetspengerTestScenario scenario) {
        return scenario.vilkårsavklaringer().get(VilkårType.BISTANDSVILKÅR).getFirst().periode();
    }

    private static Periode livsoppholdsperiode(AktivitetspengerTestScenario scenario) {
        return scenario.vilkårsavklaringer().get(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR).getFirst().periode();
    }

    private Behandling lagBehandling(AktivitetspengerTestScenario scenario) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(scenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBosted(FOM);
        return lagBehandling(scenario);
    }
}
