package no.nav.ung.sak.formidling;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.notat.NotatRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.formidling.dto.BrevbestillingDto;
import no.nav.ung.sak.formidling.dto.GenerertBrev;
import no.nav.ung.sak.formidling.dto.PartResponseDto;
import no.nav.ung.sak.formidling.kodeverk.IdType;
import no.nav.ung.sak.formidling.kodeverk.RolleType;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevGenerererTjenesteTest {

    private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    private BrevGenerererTjeneste brevGenerererTjeneste;

    @Inject
    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRepository fagsakRepository;


    String navn = "Halvorsen Halvor";
    String aktørid = "1111";

    @BeforeEach
    void setup() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);



    }

    @Test
    void genererPdf() {
        var ungdom = AktørId.dummy();
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad(ungdom);
        var behandling = scenarioBuilder.lagre(repositoryProvider);

        var pdlKlient = new PdlKlientFake("Halvor", "Halvorsen", aktørid);

        brevGenerererTjeneste = new BrevGenerererTjeneste(
            repositoryProvider.getBehandlingRepository(),
            new PersonBasisTjeneste(pdlKlient),
            new AktørTjeneste(pdlKlient)
        );

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
        PartResponseDto mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(navn);
        assertThat(mottaker.id()).isEqualTo(ungdom.getAktørId());
        assertThat(mottaker.type()).isEqualTo(IdType.AKTØRID);
        assertThat(mottaker.rolleType()).isEqualTo(RolleType.BRUKER);


        PartResponseDto gjelder = generertBrev.gjelder();
        assertThat(gjelder).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);




    }

}
