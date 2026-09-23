package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class FørstegangsbehandlingAvslagTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final String FRITEKST_BOSTED = "Du har oppgitt adresse i et land som ikke er dekket av trygdeavtalen.";
    private static final String FRITEKST_BISTAND = "Du har ikke fått et vedtak fra NAV som sier at du har behov for bistand.";
    private static final String FRITEKST_LIVSOPPHOLD = "Du får en ytelse fra en annen ordning som dekker livsoppholdet ditt.";

    FørstegangsbehandlingAvslagTest() {
        super(1, "Vi har avslått din søknad om aktivitetspenger");
    }

    @DisplayName("Avslag pga bostedsvilkåret - ytelseIkkeTilgjengeligPåBosted")
    @Test
    void avslagBosted() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttBosted(fom);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune. " +
                    "Fordi du ikke har bostedsadresse i Trondheim kommune, har vi avslått søknaden din."
            );
    }

    @DisplayName("Avslag pga bistandsvilkåret - harIkke14aVedtak")
    @Test
    void avslagBistand() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttBistand(fom, null);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "For å ha rett på aktivitetspenger må du ha et oppfølgingsvedtak etter NAV-loven § 14a"
            );
    }

    @DisplayName("Avslag pga bistandsvilkåret - harIkke14aVedtak med fritekstBrev")
    @Test
    void avslagBistand_fritekst() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttBistand(fom, FRITEKST_BISTAND);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                FRITEKST_BISTAND
            );
    }

    @DisplayName("Avslag pga bostedsvilkåret - ytelseIkkeTilgjengeligPåFolkeregistrertEllerBostedsadresse")
    @Test
    void avslagBostedFolkeregistrertEllerBostedsadresse() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttBostedFolkeregistrertEllerBostedsadresse(fom);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "bo eller være folkeregistrert i Trondheim kommune"
            );
    }

    @DisplayName("Avslag pga bostedsvilkåret - ytelseIkkePåArbeidsstedStudiested")
    @Test
    void avslagArbeidsstedStudiested() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttArbeidsstedStudiested(fom, null);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "studere eller jobbe i Trondheim kommune"
            );
    }

    @DisplayName("Avslag på både bosteds- og bistandsvilkåret - bosted omtales først")
    @Test
    void avslagBostedOgBistand() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttBostedOgBistand(fom);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "For å ha rett til aktivitetspenger må du bo i Trondheim kommune. " +
                    "Fordi du ikke har bostedsadresse i Trondheim kommune, har vi avslått søknaden din.",
                "For å ha rett på aktivitetspenger må du ha et oppfølgingsvedtak etter NAV-loven § 14a"
            );
    }

    @DisplayName("Avslag med fritekst uavhengig av avslagsårsak på bostedsvilkåret")
    @Test
    void avslagBostedFritekst() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttArbeidsstedStudiested(fom, FRITEKST_BOSTED);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                FRITEKST_BOSTED
            );
    }

    @DisplayName("Avslag pga andre livsoppholdsytelser - ytelsen navngis i brevet")
    @ParameterizedTest(name = "{0} gir teksten \"du får {1}\"")
    @CsvSource({
        "MOTTAR_ARBEIDSAVKLARINGSPENGER, arbeidsavklaringspenger",
        "MOTTAR_TILTAKSPENGER, tiltakspenger",
        "MOTTAR_KVALIFISERINGSSTØNAD, kvalifiseringsstønad",
        "MOTTAR_DAGPENGER, dagpenger",
        "MOTTAR_FORELDREPENGER, foreldrepenger",
        "MOTTAR_SVANGERSKAPSPENGER, svangerskapspenger",
        "MOTTAR_UFØRETRYGD, uføretrygd",
        "MOTTAR_INTRODUKSJONSSTØNAD, introduksjonsstønad",
        "MOTTAR_BARNEPENSJON, barnepensjon"
    })
    void avslagAndreLivsoppholdsytelser(AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String forventetYtelse) {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttAndreLivsoppholdsytelser(fom, ikkeOppfyltÅrsak, null);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "Det er fordi du får " + forventetYtelse + ". Du kan ikke få aktivitetspenger samtidig som du får en annen "
                    + "livsoppholdsytelse. Derfor har vi avslått søknaden din."
            );
    }

    @DisplayName("Avslag pga andre livsoppholdsytelser - MOTTAR_ANNEN_YTELSE navngir ingen ytelse")
    @Test
    void avslagAndreLivsoppholdsytelserAnnenYtelse() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttAndreLivsoppholdsytelser(
            fom, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE, null);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "Du kan ikke få aktivitetspenger samtidig som du får en annen livsoppholdsytelse. "
                    + "Derfor har vi avslått søknaden din."
            )
            .asPlainTextNotContains("Det er fordi du får");
    }

    @DisplayName("Avslag pga andre livsoppholdsytelser - fritekst erstatter årsakssetningen")
    @Test
    void avslagAndreLivsoppholdsytelserFritekst() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttAndreLivsoppholdsytelser(
            fom, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER, FRITEKST_LIVSOPPHOLD);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                FRITEKST_LIVSOPPHOLD
            )
            .asPlainTextNotContains("Det er fordi du får dagpenger");
    }

    @DisplayName("Vilkår som kun er avkortet omtales ikke, selv om et annet vilkår er avslått")
    @Test
    void avslagAndreLivsoppholdsytelserMedAvkortetBosted() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttAndreLivsoppholdsytelserMedAvkortetBosted(fom);

        var behandling = lagAvslåttBehandling(scenario);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG);

        assertThatHtml(generertBrev.dokument().html())
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har avslått din søknad om aktivitetspenger</h1>",
                "Du kan ikke få aktivitetspenger samtidig som du får en annen livsoppholdsytelse. "
                    + "Derfor har vi avslått søknaden din."
            )
            .asPlainTextNotContains("Trondheim")
            .asPlainTextNotContains("bosted");
    }

    private Behandling lagAvslåttBehandling(AktivitetspengerTestScenario scenario) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(scenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        behandling.avsluttBehandling();
        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        var fom = LocalDate.of(2025, 8, 1);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.avslåttBosted(fom);
        return lagAvslåttBehandling(scenario);
    }
}
