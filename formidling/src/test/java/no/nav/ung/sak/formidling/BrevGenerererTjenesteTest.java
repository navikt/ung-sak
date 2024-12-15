package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.dto.BrevbestillingDto;
import no.nav.ung.sak.formidling.dto.PartResponseDto;
import no.nav.ung.sak.formidling.kodeverk.IdType;
import no.nav.ung.sak.formidling.kodeverk.RolleType;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;

/**
 * Tester at pdf blir generert riktig.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevGenerererTjenesteTest {

    private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    private BrevGenerererTjeneste brevGenerererTjeneste;

    @Inject
    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;


    String navn = "Halvorsen Halvor";
    String aktørid = "1111";

    @BeforeEach
    void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void skal_lage_pdf_med_riktig_mottaker_navn() throws IOException {
        var ungdom = AktørId.dummy();
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad(ungdom);
        var behandling = scenarioBuilder.lagre(repositoryProvider);

        var pdlKlient = new PdlKlientFake("Halvor", "Halvorsen", aktørid);

        brevGenerererTjeneste = new BrevGenerererTjeneste(
            repositoryProvider.getBehandlingRepository(),
            new PersonBasisTjeneste(pdlKlient),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient()
        );

        // Lag innvilgelsesbrev
        var bestillBrevDto = new BrevbestillingDto(
            behandling.getId(),
            DokumentMalType.INNVILGELSE_DOK,
            behandling.getFagsak().getSaksnummer(),
            null,
            objectMapper.createObjectNode()
        );

        GenerertBrev generertBrev = brevGenerererTjeneste.generer(bestillBrevDto);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        assertThat(erPdf(generertBrev.dokument().pdf())).isTrue();
        if (System.getenv("LAGRE_PDF") != null) {
            lagrePdf(generertBrev.dokument().pdf(), generertBrev.malType().name());
        }

        PartResponseDto mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(navn);
        assertThat(mottaker.id()).isEqualTo(ungdom.getAktørId());
        assertThat(mottaker.type()).isEqualTo(IdType.AKTØRID);
        assertThat(mottaker.rolleType()).isEqualTo(RolleType.BRUKER);


        PartResponseDto gjelder = generertBrev.gjelder();
        assertThat(gjelder).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);

        var brevtekst = generertBrev.dokument().html();
        assertThatHtml(brevtekst).contains("Til: " + navn);

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


    private static void lagrePdf(byte[] data, String filename) throws IOException {
        Path directory = Files.createDirectories(Paths.get("pdfresultater"));
        File file = directory.resolve(filename + ".pdf").toFile();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

}


