package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.dokument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentType;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentIdDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokument;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokumentRepository;

@CdiDbAwareTest
class OpplæringDokumentRestTjenesteTest {

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private OpplæringDokumentRepository opplæringDokumentRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    @Inject
    public EntityManager entityManager;

    private OpplæringDokumentRestTjeneste opplæringDokumentRestTjeneste;

    private Behandling behandling;
    private final JournalpostId journalpostId = new JournalpostId("123");
    private final String dokumentInfoId = "321";
    private OpplæringDokument dokument;

    @BeforeEach
    void setup() {
        dokumentArkivTjeneste = mock(DokumentArkivTjeneste.class);
        opplæringDokumentRestTjeneste = new OpplæringDokumentRestTjeneste(behandlingRepository, opplæringDokumentRepository, dokumentArkivTjeneste);

        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medSøknadsdato(LocalDate.now());
        behandling = scenario.lagre(repositoryProvider);

        dokument = new OpplæringDokument(journalpostId, dokumentInfoId, OpplæringDokumentType.DOKUMENTASJON_AV_OPPLÆRING,
            behandling.getUuid(), behandling.getFagsak().getSaksnummer(), null, LocalDate.now(), LocalDateTime.now());
        opplæringDokumentRepository.lagre(dokument);
    }

    @Test
    void hentDokumenter() {
        List<OpplæringDokumentDto> result = opplæringDokumentRestTjeneste.hentDokumenter(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(result).hasSize(1);
        OpplæringDokumentDto dto = result.get(0);
        assertThat(Long.valueOf(dto.getId())).isEqualTo(dokument.getId());
        assertThat(dto.getType()).isEqualTo(dokument.getType());
        assertThat(dto.getDatert()).isEqualTo(dokument.getDatert());
        assertThat(dto.isFremhevet()).isTrue();
        assertThat(dto.getLinks()).hasSize(1);
        assertThat(dto.getLinks().get(0).getRel()).isEqualTo("opplæring-dokument-innhold");
    }

    @Test
    void hentDokumentinnhold() {
        when(dokumentArkivTjeneste.hentDokumnet(journalpostId, dokumentInfoId)).thenReturn(new byte[0]);

        Response response = opplæringDokumentRestTjeneste.hentDokumentinnhold(new BehandlingUuidDto(behandling.getUuid()), new SykdomDokumentIdDto(dokument.getId().toString()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
