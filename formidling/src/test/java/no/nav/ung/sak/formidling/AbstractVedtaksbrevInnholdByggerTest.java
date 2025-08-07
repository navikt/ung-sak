package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisRequest;
import no.nav.ung.sak.test.util.UngTestRepositories;
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
 * Har fellestester og noe utility funksjoner for å generere vedtaksbrev.
 * Test for brevtekster. Bruker html for å validere.
 * For manuell verifikasjon av pdf/html kan env variabel LAGRE settes til PDF eller HTML .
 */

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
abstract class AbstractVedtaksbrevInnholdByggerTest {

    @Inject
    protected EntityManager entityManager;

    @Inject
    protected PdlKlientFake pdl;


    private final int forventetAntallPdfSider;
    private final String forventetPdfHovedoverskrift;

    private TestInfo testInfo;

    protected String fnr;
    protected UngTestRepositories ungTestRepositories;

    @Inject
    protected VedtaksbrevTjeneste vedtaksbrevTjeneste;

    AbstractVedtaksbrevInnholdByggerTest(int forventetAntallPdfSider, String forventetPdfHovedoverskrift) {
        this.forventetAntallPdfSider = forventetAntallPdfSider;
        this.forventetPdfHovedoverskrift = forventetPdfHovedoverskrift;
    }


    @BeforeEach
    void baseSetup(TestInfo testInfo) {
        this.testInfo = testInfo;
        this.fnr = pdl.fnr();
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
    }


    @Test
    @DisplayName("Verifiserer formatering på overskrifter")
    void verifiserOverskrifter() {
        var behandling = lagScenarioForFellesTester();

        List<GenerertBrev> brev = vedtaksbrevTjeneste.forhåndsvis(lagForhåndsvisInput(behandling.getId(), true));
        assertThat(brev).hasSize(1);
        GenerertBrev generertBrev = brev.getFirst();

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst).containsHtmlSubSequenceOnce(
            "<h2>Du har rett til å klage</h2>",
            "<h2>Du har rett til innsyn</h2>",
            "<h2>Trenger du mer informasjon?</h2>"
        );

    }

    private static VedtaksbrevForhåndsvisRequest lagForhåndsvisInput(Long behandlingId, boolean kunHtml) {
        return new VedtaksbrevForhåndsvisRequest(behandlingId, null, kunHtml, null);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PDF", matches = "true")
    void pdfStrukturTest() throws IOException {
        var behandling = lagScenarioForFellesTester();

        List<GenerertBrev> brev = vedtaksbrevTjeneste.forhåndsvis(lagForhåndsvisInput(behandling.getId(), false));
        assertThat(brev).hasSize(1);
        GenerertBrev generertBrev = brev.getFirst();

        var pdf = generertBrev.dokument().pdf();

        try (PDDocument pdDocument = Loader.loadPDF(pdf)) {
            assertThat(pdDocument.getNumberOfPages()).isEqualTo(forventetAntallPdfSider);
            String pdfTekst = new PDFTextStripper().getText(pdDocument);
            assertThat(pdfTekst).isNotEmpty();
            assertThat(pdfTekst).contains(forventetPdfHovedoverskrift);
        }

    }

    final protected GenerertBrev genererVedtaksbrev(Long behandlingId) {
        return genererVedtaksbrev(behandlingId, testInfo, vedtaksbrevTjeneste);
    }


    /**
     * Lager vedtaksbrev med mulighet for å lagre pdf lokalt hvis env variabel LAGRE_PDF er satt.
     */
    static protected GenerertBrev genererVedtaksbrev(Long behandlingId, TestInfo testInfo, VedtaksbrevTjeneste vedtaksbrevTjeneste) {
        String lagre = System.getenv("LAGRE");

        if (lagre == null) {
            List<GenerertBrev> forhåndsvis = vedtaksbrevTjeneste.forhåndsvis(lagForhåndsvisInput(behandlingId, true));
            assertThat(forhåndsvis).hasSize(1);
            return forhåndsvis.getFirst();
        }

        List<GenerertBrev> forhåndsvis = vedtaksbrevTjeneste.forhåndsvis(lagForhåndsvisInput(behandlingId, !lagre.equals("PDF")));
        assertThat(forhåndsvis).hasSize(1);
        GenerertBrev generertBrev = forhåndsvis.getFirst();

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
     * Brukes av fellestester i base klasse
     */
    protected abstract Behandling lagScenarioForFellesTester();


}
