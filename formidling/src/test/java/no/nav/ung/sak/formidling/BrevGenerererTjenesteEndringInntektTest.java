package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

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
                new RapportertInntektMapper(abakusInMemoryInntektArbeidYtelseTjeneste),
                ungdomsytelseGrunnlagRepository);

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(prosessTriggereRepository, new UngdomsytelseSøknadsperiodeTjeneste(ungdomsytelseStartdatoRepository, ungdomsprogramPeriodeTjeneste, repositoryProvider.getBehandlingRepository())),
            tilkjentYtelseRepository);

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
    @DisplayName("Verifiserer faste tekster og mottaker")
    void skalHaAlleStandardtekster() {
        UngTestScenario ungTestscenario = BrevScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));
        var behandling = lagScenario(ungTestscenario);

        GenerertBrev generertBrev = genererVedtaksbrevBrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        VedtaksbrevStandardTekstVerifiserer.verifiserStandardVedtaksbrevTekster(brevtekst, fnr, ungTestscenario);

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

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrevBrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst).containsHtmlOnceInSequence(
            "<h1>Vi har endret ungdomsytelsen din</h1>"
        ).containsSentencesOnceInSequence(
            "Du har meldt inn inntekt på 10 000 kroner fra 1. desember 2024 til 31. desember 2024.",
            "Nav har derfor redusert utbetalingen din for neste perioden til 7 393 kroner.",
            "Nav reduserer utbetalt beløp med 66 prosent av innmeldt inntekt.",
            "Dette tilsvarer en reduksjon på 6 600 kroner.",
            "Dagsatsen blir redusert fra 636 kroner til 336 kroner.",
            "Vedtaket er gjort etter folketrygdloven § X-Y."
        );

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


