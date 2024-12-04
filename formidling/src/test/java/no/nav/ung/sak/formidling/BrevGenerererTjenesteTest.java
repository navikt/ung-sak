package no.nav.ung.sak.formidling;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.dto.BrevbestillingDto;
import no.nav.ung.sak.formidling.dto.GenerertBrev;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;

class BrevGenerererTjenesteTest {

    private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    private BrevGenerererTjeneste brevGenerererTjeneste = new BrevGenerererTjeneste();


    @Test
    void genererPdf() {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenarioBuilder.lagMocked();


        // Lag innvilgelsesbrev
        var bestillBrevDto = new BrevbestillingDto(
            behandling.getId(),
            DokumentMalType.INNVILGELSE_DOK,
            behandling.getFagsak().getSaksnummer(),
            null,
            objectMapper.createObjectNode()
        );

        GenerertBrev generertBrev = brevGenerererTjeneste.genererPdf(bestillBrevDto);

        assertThat(new String(generertBrev.pdfData())).isEqualTo("123");
        assertThat(generertBrev.mottaker().navn()).isEqualTo("halvorsen");
        assertThat(generertBrev.gjelder().navn()).isEqualTo("halvorsen");
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);




    }

}
