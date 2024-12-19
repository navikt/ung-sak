package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.dto.Brevbestilling;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;

/**
 * Test for brevtekster for innvilgelse. Lager ikke pdf, men bruker html for å validere.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InnvilgelseTest {

    private BrevGenerererTjeneste brevGenerererTjeneste;
    private ObjectMapper objectMapper = JsonUtils.getObjectMapper();


    @Inject
    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;

    String navn = "Halvorsen Halvor";

    @BeforeEach
    void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var pdlKlient = new PdlKlientFake("Halvor", "Halvorsen", "1111");

        brevGenerererTjeneste = new BrevGenerererTjeneste(
            repositoryProvider.getBehandlingRepository(),
            new PersonBasisTjeneste(pdlKlient),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(true)
        );
    }

    @Test
    void standard_innvilgelse() {
        var ungdom = AktørId.dummy();
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad(ungdom);
        var behandling = scenarioBuilder.lagre(repositoryProvider);

        var bestillBrevDto = lagBestilling(behandling);
        GenerertBrev generertBrev = brevGenerererTjeneste.generer(bestillBrevDto);

        var brevtekst = generertBrev.dokument().html();
        assertThatHtml(brevtekst).contains("<h1>NAV har innvilget søknaden din om ungdomsytelse</h1>");
        assertThatHtml(brevtekst).contains("Til: " + navn);

    }

    private Brevbestilling lagBestilling(Behandling behandling) {
        return new Brevbestilling(
            behandling.getId(),
            DokumentMalType.INNVILGELSE_DOK,
            behandling.getFagsak().getSaksnummer(),
            null,
            objectMapper.createObjectNode()
        );
    }


}


