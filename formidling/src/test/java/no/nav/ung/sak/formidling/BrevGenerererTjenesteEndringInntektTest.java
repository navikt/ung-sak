package no.nav.ung.sak.formidling;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederImpl;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
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
    private BehandlingRepositoryProvider repositoryProvider;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private PersonopplysningRepository personopplysningRepository;
    private AbakusInMemoryInntektArbeidYtelseTjeneste abakusInMemoryInntektArbeidYtelseTjeneste;

    PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    String fnr = pdlKlient.fnr();
    private TestInfo testInfo;
    private TilkjentYtelseRepository tilkjentYtelseRepository;


    @BeforeEach
    void setup(TestInfo testInfo) {
        this.testInfo = testInfo;
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        ungdomsytelseStartdatoRepository = new UngdomsytelseStartdatoRepository(entityManager);

        abakusInMemoryInntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        brevGenerererTjeneste = lagBrevGenererTjeneste(System.getenv("LAGRE_PDF") == null);
    }

    private BrevGenerererTjeneste lagBrevGenererTjeneste(boolean ignorePdf) {
        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository);

        var endringInnholdBygger =
            new EndringRapportertInntektInnholdBygger(tilkjentYtelseRepository,
                new RapportertInntektMapper(abakusInMemoryInntektArbeidYtelseTjeneste, new MånedsvisTidslinjeUtleder(new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository), repositoryProvider.getBehandlingRepository()))
            );

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(prosessTriggereRepository, new UngdomsytelseSøknadsperiodeTjeneste(ungdomsytelseStartdatoRepository, ungdomsprogramPeriodeTjeneste, repositoryProvider.getBehandlingRepository())),
            tilkjentYtelseRepository, repositoryProvider.getVilkårResultatRepository());

        Instance<VedtaksbrevInnholdBygger> innholdByggere = new UnitTestLookupInstanceImpl<>(endringInnholdBygger);

        return new BrevGenerererTjenesteImpl(
            repositoryProvider.getBehandlingRepository(),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(ignorePdf),
            personopplysningRepository,
            new VedtaksbrevRegler(
                repositoryProvider.getBehandlingRepository(), innholdByggere, detaljertResultatUtleder));
    }

    @Test()
    @DisplayName("Verifiserer formatering på overskrifter")
    void verifiserOverskrifter() {
        UngTestScenario ungTestscenario = BrevScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));
        var behandling = lagScenario(ungTestscenario);

        GenerertBrev generertBrev = genererVedtaksbrevBrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        VedtaksbrevVerifikasjon.verifiserStandardOverskrifter(brevtekst);

    }

    @NotNull
    private UngTestRepositories lagUngTestRepositories() {
        return new UngTestRepositories(repositoryProvider, ungdomsytelseGrunnlagRepository, ungdomsprogramPeriodeRepository, ungdomsytelseStartdatoRepository, tilkjentYtelseRepository, prosessTriggereRepository);
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

        GenerertBrev generertBrev = genererVedtaksbrevBrev(behandling.getId());
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

        GenerertBrev generertBrev = genererVedtaksbrevBrev(behandling.getId());
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
        assertThat(genererVedtaksbrevBrev(behandling.getId())).isNull();

    }


    @Test
    void pdfStrukturTest() throws IOException {

        //Lager ny fordi default PdfgenKlient lager ikke pdf
        var brevGenerererTjeneste = lagBrevGenererTjeneste(false);

        var behandling = lagScenario(
            BrevScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1)));


        GenerertBrev generertBrev = genererVedtaksbrevBrev(behandling.getId(), brevGenerererTjeneste);

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

        UngTestRepositories repositories = lagUngTestRepositories();
        var behandling = scenarioBuilder.buildOgLagreMedUng(repositories);

        abakusInMemoryInntektArbeidYtelseTjeneste.lagreOppgittOpptjening(
            behandling.getId(),
            ungTestscenario.abakusInntekt()
        );

        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();


        return behandling;
    }

    private GenerertBrev genererVedtaksbrevBrev(Long behandlingId) {
        return genererVedtaksbrevBrev(behandlingId, brevGenerererTjeneste);
    }


    private GenerertBrev genererVedtaksbrevBrev(Long behandlingId, BrevGenerererTjeneste brevGenerererTjeneste1) {
        GenerertBrev generertBrev = brevGenerererTjeneste1.genererVedtaksbrev(behandlingId);
        if (System.getenv("LAGRE_PDF") != null) {
            BrevUtils.lagrePdf(generertBrev, testInfo);
        }
        return generertBrev;
    }


}


