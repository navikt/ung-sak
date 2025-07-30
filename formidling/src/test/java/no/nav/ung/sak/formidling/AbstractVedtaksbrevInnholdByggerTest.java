package no.nav.ung.sak.formidling;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederImpl;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegler;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestMultiLookupInstanceImpl;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test for brevtekster. Bruker html for å validere.
 * For manuell verifikasjon av pdf/html kan env variabel LAGRE settes til PDF eller HTML .
 */

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
abstract class AbstractVedtaksbrevInnholdByggerTest {

    @Inject
    protected EntityManager entityManager;

    protected final PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    private final int forventetAntallPdfSider;
    private final String forventetPdfHovedoverskrift;

    private TestInfo testInfo;

    protected String fnr = pdlKlient.fnr();
    protected UngTestRepositories ungTestRepositories;
    protected VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste;

    AbstractVedtaksbrevInnholdByggerTest(int forventetAntallPdfSider, String forventetPdfHovedoverskrift) {
        this.forventetAntallPdfSider = forventetAntallPdfSider;
        this.forventetPdfHovedoverskrift = forventetPdfHovedoverskrift;
    }


    @BeforeEach
    void baseSetup(TestInfo testInfo) {
        this.testInfo = testInfo;
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        vedtaksbrevGenerererTjeneste = lagDefaultBrevGenererTjeneste(lagVedtaksbrevByggerStrategier());
    }




    private VedtaksbrevGenerererTjeneste lagDefaultBrevGenererTjeneste(
        List<VedtaksbrevInnholdbyggerStrategy> vedtaksbrevInnholdbyggerStrategies) {
        return lagBrevGenererTjeneste(ungTestRepositories, pdlKlient, false, vedtaksbrevInnholdbyggerStrategies);
    }

    protected static VedtaksbrevGenerererTjeneste lagBrevGenererTjeneste(UngTestRepositories ungTestRepositories, PdlKlientFake pdlKlient, Boolean enableAutoBrevVedBarnDødsfall, List<VedtaksbrevInnholdbyggerStrategy> vedtaksbrevInnholdbyggerStrategies) {
        var repositoryProvider = ungTestRepositories.repositoryProvider();

        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository = ungTestRepositories.ungdomsprogramPeriodeRepository();
        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository, ungTestRepositories.ungdomsytelseStartdatoRepository());

        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(), new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(), ungdomsprogramPeriodeTjeneste, behandlingRepository)),
            ungTestRepositories.tilkjentYtelseRepository(), repositoryProvider.getVilkårResultatRepository());

        Instance<VedtaksbrevInnholdbyggerStrategy> innholdByggerStrategier = new UnitTestMultiLookupInstanceImpl<>(
            vedtaksbrevInnholdbyggerStrategies
        );

        ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger = new ManueltVedtaksbrevInnholdBygger(ungTestRepositories.vedtaksbrevValgRepository());

        return new VedtaksbrevGenerererTjenesteImpl(
            behandlingRepository,
            new PdfGenKlient(),
            new VedtaksbrevRegler(
                behandlingRepository,
                detaljertResultatUtleder,
                innholdByggerStrategier,
                manueltVedtaksbrevInnholdBygger),
            ungTestRepositories.vedtaksbrevValgRepository(),
            manueltVedtaksbrevInnholdBygger,
            new BrevMottakerTjeneste(new AktørTjeneste(pdlKlient), repositoryProvider.getPersonopplysningRepository()));
    }

    @Test
    @DisplayName("Verifiserer formatering på overskrifter")
    void verifiserOverskrifter() {
        var behandling = lagScenarioForFellesTester();

        Long behandlingId = (behandling.getId());
        GenerertBrev generertBrev = vedtaksbrevGenerererTjeneste.genererVedtaksbrevForBehandling(behandlingId, true);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst).containsHtmlSubSequenceOnce(
            "<h2>Du har rett til å klage</h2>",
            "<h2>Du har rett til innsyn</h2>",
            "<h2>Trenger du mer informasjon?</h2>"
        );

    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PDF", matches = "true")
    void pdfStrukturTest() throws IOException {
        var behandling = lagScenarioForFellesTester();

        GenerertBrev generertBrev = vedtaksbrevGenerererTjeneste.genererVedtaksbrevForBehandling(behandling.getId(), false);

        var pdf = generertBrev.dokument().pdf();

        try (PDDocument pdDocument = Loader.loadPDF(pdf)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(forventetAntallPdfSider);
            String pdfTekst = new PDFTextStripper().getText(pdDocument);
            assertThat(pdfTekst).isNotEmpty();
            assertThat(pdfTekst).contains(forventetPdfHovedoverskrift);
        }

    }

    /**
     * Lager vedtaksbrev med mulighet for å lagre pdf lokalt hvis env variabel LAGRE_PDF er satt.
     */
    final protected GenerertBrev genererVedtaksbrev(Long behandlingId) {
        return genererVedtaksbrev(vedtaksbrevGenerererTjeneste, behandlingId);
    }


    /**
     * Mulighet for å bruke egen VedtaksbrevGenerererTjeneste
     */
    final protected GenerertBrev genererVedtaksbrev(VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste, Long behandlingId) {
        String lagre = System.getenv("LAGRE");
        if (lagre == null) {
            return vedtaksbrevGenerererTjeneste.genererVedtaksbrevForBehandling(behandlingId, true);
        }

        GenerertBrev generertBrev = vedtaksbrevGenerererTjeneste.genererVedtaksbrevForBehandling(behandlingId, !lagre.equals("PDF"));

        switch (lagre) {
            case "PDF":
                BrevTestUtils.lagrePdf(generertBrev, testInfo);
                break;
            case "HTML":
                BrevTestUtils.lagreHtml(generertBrev, testInfo);
                break;
            default:
                throw new IllegalArgumentException("Ugyldig verdi for LAGRE: " + lagre + ". Forventet 'PDF' eller 'HTML'.");
        }

        return generertBrev;
    }


    /**
     * Brukes for å lage BrevGenerererTjeneste
     */
    protected abstract List<VedtaksbrevInnholdbyggerStrategy> lagVedtaksbrevByggerStrategier();




    /**
     * Brukes av fellestester i base klasse
     */
    protected abstract Behandling lagScenarioForFellesTester();


}
