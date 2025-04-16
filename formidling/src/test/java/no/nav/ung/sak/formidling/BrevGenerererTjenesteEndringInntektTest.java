package no.nav.ung.sak.formidling;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.innhold.ManuellVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederImpl;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for brevtekster for innvilgelse. Bruker html for å validere.
 * For manuell verifikasjon av pdf kan env variabel LAGRE_PDF brukes.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevGenerererTjenesteEndringInntektTest {

    private BrevGenerererTjeneste brevGenerererTjeneste;

    @Inject
    private EntityManager entityManager;
    private UngTestRepositories ungTestRepositories;


    PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    String fnr = pdlKlient.fnr();
    private TestInfo testInfo;


    @BeforeEach
    void setup(TestInfo testInfo) {
        this.testInfo = testInfo;
        ungTestRepositories = BrevUtils.lagAlleUngTestRepositories(entityManager);
        brevGenerererTjeneste = lagBrevGenererTjeneste();
    }

    private BrevGenerererTjeneste lagBrevGenererTjeneste() {
        var repositoryProvider = ungTestRepositories.repositoryProvider();
        var tilkjentYtelseRepository = ungTestRepositories.tilkjentYtelseRepository();

        var ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository());

        var endringInnholdBygger =
            new EndringRapportertInntektInnholdBygger(tilkjentYtelseRepository,
                new RapportertInntektMapper(ungTestRepositories.abakusInMemoryInntektArbeidYtelseTjeneste(), new MånedsvisTidslinjeUtleder(ungdomsprogramPeriodeTjeneste, repositoryProvider.getBehandlingRepository()))
            );

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(), new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(), ungdomsprogramPeriodeTjeneste, repositoryProvider.getBehandlingRepository())),
            tilkjentYtelseRepository, repositoryProvider.getVilkårResultatRepository());

        Instance<VedtaksbrevInnholdBygger> innholdByggere = new UnitTestLookupInstanceImpl<>(endringInnholdBygger);

        return new BrevGenerererTjenesteImpl(
            repositoryProvider.getBehandlingRepository(),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            repositoryProvider.getPersonopplysningRepository(),
            new VedtaksbrevRegler(
                repositoryProvider.getBehandlingRepository(), innholdByggere, detaljertResultatUtleder),
            ungTestRepositories.vedtaksbrevValgRepository(), new ManuellVedtaksbrevInnholdBygger(ungTestRepositories.vedtaksbrevValgRepository()));
    }

    @Test()
    @DisplayName("Verifiserer formatering på overskrifter")
    void verifiserOverskrifter() {
        UngTestScenario ungTestscenario = BrevScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));
        var behandling = lagScenario(ungTestscenario);

        Long behandlingId = behandling.getId();
        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrev(behandlingId, true);

        var brevtekst = generertBrev.dokument().html();

        VedtaksbrevVerifikasjon.verifiserStandardOverskrifter(brevtekst);

    }


    @DisplayName("Endringsbrev med periode, innrapportert inntekt, reduksjon og utbetaling")
    @Test
    void standardEndringRapportertInntekt() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.endringMedInntektPå10k_19år(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsytelsen din " +
                "Du får 8 029 kroner i ungdomsytelse for perioden fra 1. januar 2025 til 31. januar 2025. " +
                "Det er fordi du har hatt en inntekt på 10 000 kroner i denne perioden. " +
                "Pengene får du utbetalt før den 10. denne måneden. " +
                "Når du har en inntekt, får du mindre penger i ungdomsytelse. " +
                "Vi regner ut hva 66 prosent av inntekten din er hver måned, og så trekker vi dette beløpet fra pengene du får i ungdomsytelsen for den måneden. " +
                "Likevel får du til sammen mer penger når du både har en inntekt og får ungdomsytelse, enn hvis du bare hadde fått penger gjennom ungdomsytelsen. " +
                "Se eksempel på hvordan vi regner ut ungdomsytelsen basert på inntekt i Ungdomsportalen. " +
                "Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. ");

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsytelsen din</h1>",
                "Se <a title=\"utregningseksempler\" href=\"https://nav.no/ungdomsportal/beregning\">eksempel</a> på hvordan vi regner ut ungdomsytelsen basert på inntekt i Ungdomsportalen."
            );
    }

    @DisplayName("Endringsbrev med flere perioder")
    @Test
    void melder_inntekt_for_flere_mnd() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.endringMedInntektPå10k_flere_mnd_19år(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            "Vi har endret ungdomsytelsen din " +
                "Du får 14 150 kroner i ungdomsytelse for perioden fra 1. januar 2025 til 28. februar 2025. " +
                "Det er fordi du har hatt en inntekt på 20 000 kroner i denne perioden. " +
                "Pengene får du utbetalt før den 10. denne måneden. " +
                "Vi har registrert følgende inntekter på deg: " +
                "Fra 1. januar 2025 til 31. januar 2025 har du hatt en inntekt på 10 000 kroner. " +
                "Fra 1. februar 2025 til 28. februar 2025 har du hatt en inntekt på 10 000 kroner. " +
                "Når du har en inntekt, får du mindre penger i ungdomsytelse. " +
                "Vi regner ut hva 66 prosent av inntekten din er hver måned, og så trekker vi dette beløpet fra pengene du får i ungdomsytelsen for den måneden. " +
                "Likevel får du til sammen mer penger når du både har en inntekt og får ungdomsytelse, enn hvis du bare hadde fått penger gjennom ungdomsytelsen. " +
                "Se eksempel på hvordan vi regner ut ungdomsytelsen basert på inntekt i Ungdomsportalen. " +
                "Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. ");

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsytelsen din</h1>",
                "Se <a title=\"utregningseksempler\" href=\"https://nav.no/ungdomsportal/beregning\">eksempel</a> på hvordan vi regner ut ungdomsytelsen basert på inntekt i Ungdomsportalen."
            );
    }

    @DisplayName("Ingen brev ved ingen rapportert inntekt og ingen inntekt")
    @Test
    void full_ungdomsprogram_med_ingen_rapportert_inntekt_gir_ingen_brev() {
        LocalDate fom = LocalDate.of(2024, 12, 1);
        var ungTestGrunnlag = BrevScenarioer.endring0KrInntekt_19år(fom);
        var behandling = lagScenario(ungTestGrunnlag);
        assertThat(brevGenerererTjeneste.genererVedtaksbrev(behandling.getId(), true)).isNull();

    }


    @Test
    void pdfStrukturTest() throws IOException {
        var behandling = lagScenario(
            BrevScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1)));


        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrev(behandling.getId(), false);

        var pdf = generertBrev.dokument().pdf();

        try (PDDocument pdDocument = Loader.loadPDF(pdf)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(1);
            String pdfTekst = new PDFTextStripper().getText(pdDocument);
            assertThat(pdfTekst).isNotEmpty();
            assertThat(pdfTekst).contains("Vi har endret ungdomsytelsen din");
        }

    }


    private Behandling lagScenario(UngTestScenario ungTestscenario) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        UngTestRepositories repositories = ungTestRepositories;
        var behandling = scenarioBuilder.buildOgLagreMedUng(repositories);

        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();


        return behandling;
    }


    private GenerertBrev genererVedtaksbrev(Long behandlingId) {
        return BrevUtils.genererBrevOgLagreHvisEnabled(testInfo, behandlingId, brevGenerererTjeneste);
    }

}


