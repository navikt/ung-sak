package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.mottaker.PdlPerson;
import no.nav.ung.sak.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevGenerererTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tester at pdf blir generert riktig.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VedtaksbrevGenerererTjenesteTest {

    private UngTestRepositories ungTestRepositories;

    @Inject
    private EntityManager entityManager;

    @Inject
    private VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste;

    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
    }


    @Test
    void skal_lage_vedtakspdf() {

        var scenario = FørstegangsbehandlingScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);
        var ungTestGrunnlag = scenario.getUngTestGrunnlag();
        var behandling = scenario.getBehandling();


        GenerertBrev generertBrev = vedtaksbrevGenerererTjeneste.genererVedtaksbrevForBehandling(behandling.getId(), false);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        assertThat(erPdf(generertBrev.dokument().pdf())).isTrue();

        PdlPerson mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(ungTestGrunnlag.navn());
        assertThat(mottaker.aktørId().getAktørId()).isEqualTo(behandling.getAktørId().getAktørId());

        PdlPerson gjelder = generertBrev.gjelder();
        assertThat(gjelder).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);

        var brevtekst = generertBrev.dokument().html();
        assertThat(brevtekst).contains("Til: " + ungTestGrunnlag.navn());

    }

    public static boolean erPdf(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 5) {
            return false; // Not enough data to check
        }

        // Bruker StandardCharsets.US_ASCII for å sikre konsekvent tolkning av PDF-magiske tall ("%PDF-"),
        // siden dette alltid er innenfor ASCII-tegnsettet, uavhengig av plattformens standard tegnsett.
        String magicNumber = new String(fileBytes, 0, 5, StandardCharsets.US_ASCII);
        return "%PDF-".equals(magicNumber);
    }


    //TODO lage hindre test
}


