package no.nav.ung.sak.formidling;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.EndringHøySatsInnholdBygger;
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
class BrevGenerererTjenesteEndringHøySatsTest {

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

        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository());

        var endringInnholdBygger = new EndringHøySatsInnholdBygger(ungTestRepositories.ungdomsytelseGrunnlagRepository());

        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(), new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(), ungdomsprogramPeriodeTjeneste, behandlingRepository)),
            ungTestRepositories.tilkjentYtelseRepository(), repositoryProvider.getVilkårResultatRepository());

        Instance<VedtaksbrevInnholdBygger> innholdByggere = new UnitTestLookupInstanceImpl<>(endringInnholdBygger);

        return new BrevGenerererTjenesteImpl(
            behandlingRepository,
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            repositoryProvider.getPersonopplysningRepository(),
            new VedtaksbrevRegler(
                behandlingRepository, innholdByggere, detaljertResultatUtleder), ungTestRepositories.vedtaksbrevValgRepository(), new ManuellVedtaksbrevInnholdBygger(ungTestRepositories.vedtaksbrevValgRepository()));
    }

    @Test()
    @DisplayName("Verifiserer formatering på overskrifter")
    void verifiserOverskrifter() {
        UngTestScenario ungTestscenario = BrevScenarioer.endring25År(LocalDate.of(1999, 3, 25));
        var behandling = lagScenario(ungTestscenario);

        Long behandlingId = (behandling.getId());
        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrevForBehandling(behandlingId, true);

        var brevtekst = generertBrev.dokument().html();

        VedtaksbrevVerifikasjon.verifiserStandardOverskrifter(brevtekst);

    }

    @DisplayName("Endringsbrev for overgang til høy sats")
    @Test
    void standardEndringHøySats() {
        LocalDate fødselsdato = LocalDate.of(2000, 3, 25);
        var ungTestGrunnlag = BrevScenarioer.endring25År(fødselsdato);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du fyller 25 år \
                Du får mer penger gjennom ungdomsprogramytelsen fordi du fyller 25 år 25. mars 2025. \
                Fra og med denne datoen får du 974 kroner per dag, utenom lørdag og søndag. \
                Se eksempel i Ungdomsportalen på hvordan vi regner ut ungdomsprogramytelsen når du er over 25 år. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """);


        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_HØY_SATS);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i ungdomsprogramytelse fordi du fyller 25 år</h1>",
                "<a href=\"https://www.nav.no/ungdomsprogramytelse/beregning\">Se eksempel i Ungdomsportalen på hvordan vi regner ut ungdomsprogramytelsen når du er over 25 år.</a>"
            );

    }

    @Test
    void medBarnetillegg() {
        LocalDate fødselsdato = LocalDate.of(2000, 3, 25);
        var ungTestGrunnlag = BrevScenarioer.endring25ÅrMedBarn(fødselsdato);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i ungdomsprogramytelse fordi du fyller 25 år \
                Du får mer penger gjennom ungdomsprogramytelsen fordi du fyller 25 år 25. mars 2025. \
                Fra og med denne datoen får du 1 011 kroner per dag, utenom lørdag og søndag. \
                Se eksempel i Ungdomsportalen på hvordan vi regner ut ungdomsprogramytelsen når du er over 25 år. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """);


        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_HØY_SATS);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i ungdomsprogramytelse fordi du fyller 25 år</h1>"
            );

    }

    @Test
    void pdfStrukturTest() throws IOException {
        var behandling = lagScenario(
            BrevScenarioer.endring25År(LocalDate.of(2024, 12, 1)));


        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrevForBehandling(behandling.getId(), false);

        var pdf = generertBrev.dokument().pdf();

        try (PDDocument pdDocument = Loader.loadPDF(pdf)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(1);
            String pdfTekst = new PDFTextStripper().getText(pdDocument);
            assertThat(pdfTekst).isNotEmpty();
            assertThat(pdfTekst).contains("Du får mer i ungdomsprogramytelse fordi du fyller 25 år");
        }

    }

    private Behandling lagScenario(UngTestScenario ungTestscenario) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);


        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();


        return behandling;
    }


    private GenerertBrev genererVedtaksbrev(Long behandlingId) {
        return BrevUtils.genererBrevOgLagreHvisEnabled(testInfo, behandlingId, brevGenerererTjeneste);
    }



}


