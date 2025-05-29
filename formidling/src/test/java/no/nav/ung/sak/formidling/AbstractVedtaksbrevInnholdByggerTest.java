package no.nav.ung.sak.formidling;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.ManuellVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederImpl;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test for brevtekster. Bruker html for å validere.
 * For manuell verifikasjon av pdf kan env variabel LAGRE_PDF brukes.
 */

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
abstract class AbstractVedtaksbrevInnholdByggerTest {

    @Inject
    protected EntityManager entityManager;

    private final PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    private final int forventetAntallPdfSider;
    private final String forventetPdfHovedoverskrift;

    private TestInfo testInfo;

    protected String fnr = pdlKlient.fnr();
    protected UngTestRepositories ungTestRepositories;
    protected BrevGenerererTjeneste brevGenerererTjeneste;

    AbstractVedtaksbrevInnholdByggerTest(int forventetAntallPdfSider, String forventetPdfHovedoverskrift) {
        this.forventetAntallPdfSider = forventetAntallPdfSider;
        this.forventetPdfHovedoverskrift = forventetPdfHovedoverskrift;
    }


    @BeforeEach
    void baseSetup(TestInfo testInfo) {
        this.testInfo = testInfo;
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        brevGenerererTjeneste = lagBrevGenererTjeneste(lagVedtaksbrevInnholdBygger());
    }

    private BrevGenerererTjeneste lagBrevGenererTjeneste(VedtaksbrevInnholdBygger vedtaksbrevInnholdBygger) {
        var repositoryProvider = ungTestRepositories.repositoryProvider();

        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository());

        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(), new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(), ungdomsprogramPeriodeTjeneste, behandlingRepository)),
            ungTestRepositories.tilkjentYtelseRepository(), repositoryProvider.getVilkårResultatRepository());

        Instance<VedtaksbrevInnholdBygger> innholdByggere = new UnitTestLookupInstanceImpl<>(vedtaksbrevInnholdBygger);

        return new BrevGenerererTjenesteImpl(
            behandlingRepository,
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            repositoryProvider.getPersonopplysningRepository(),
            new VedtaksbrevRegler(
                behandlingRepository, innholdByggere, detaljertResultatUtleder), ungTestRepositories.vedtaksbrevValgRepository(), new ManuellVedtaksbrevInnholdBygger(ungTestRepositories.vedtaksbrevValgRepository()));
    }

    @Test
    @DisplayName("Verifiserer formatering på overskrifter")
    void verifiserOverskrifter() {
        var behandling = lagScenarioForFellesTester();

        Long behandlingId = (behandling.getId());
        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrevForBehandling(behandlingId, true);

        var brevtekst = generertBrev.dokument().html();

        VedtaksbrevVerifikasjon.verifiserStandardOverskrifter(brevtekst);

    }

    @Test
    void pdfStrukturTest() throws IOException {
        var behandling = lagScenarioForFellesTester();

        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrevForBehandling(behandling.getId(), false);

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
        return BrevTestUtils.genererBrevOgLagreHvisEnabled(testInfo, behandlingId, brevGenerererTjeneste);
    }

    /**
     * Brukes for å lage BrevGenerererTjeneste
     */
    protected abstract VedtaksbrevInnholdBygger lagVedtaksbrevInnholdBygger();

    /**
     * Brukes av fellestester i base klasse
     */
    protected abstract Behandling lagScenarioForFellesTester();


}
