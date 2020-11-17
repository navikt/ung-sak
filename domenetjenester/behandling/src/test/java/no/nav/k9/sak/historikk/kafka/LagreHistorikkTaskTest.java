package no.nav.k9.sak.historikk.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class LagreHistorikkTaskTest {

    @Inject
    private EntityManager entityManager;

    String melding = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("historikkinnslagmelding.json").toURI())));
    private HistorikkRepository historikkRepository;
    private Behandling behandling;
    private HistorikkFraDtoMapper historikkFraDtoMapper;
    private LagreHistorikkTask task;
    private AbstractTestScenario<?> scenario;

    public LagreHistorikkTaskTest() throws IOException, URISyntaxException {
    }

    @BeforeEach
    public void setup() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.lagre(repositoryProvider);
        behandling = scenario.getBehandling();
        melding = melding.replace("PLACEHOLDER-UUID", behandling.getUuid().toString());
        historikkRepository = new HistorikkRepository(entityManager);
        historikkFraDtoMapper = new HistorikkFraDtoMapper(repositoryProvider.getBehandlingRepository(), repositoryProvider.getFagsakRepository());
        task = new LagreHistorikkTask(historikkRepository, historikkFraDtoMapper);
    }


    @Test
    public void skal_parse_melding_og_lagre_historikkinnslag() {
        ProsessTaskData data = new ProsessTaskData(LagreHistorikkTask.TASKTYPE);
        data.setPayload(melding);
        task.doTask(data);
        List<Historikkinnslag> historikkinnslag = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikkinnslag).hasSize(1);
        assertThat(historikkinnslag.get(0).getType()).isEqualTo(HistorikkinnslagType.BREV_SENT);
        assertThat(historikkinnslag.get(0).getHistorikkTid()).isNotNull();
        assertThat(historikkinnslag.get(0).getOpprettetISystem()).isEqualTo("FP-FORMIDLING");
        List<HistorikkinnslagDokumentLink> linker = historikkinnslag.get(0).getDokumentLinker();
        assertThat(linker).hasSize(1);
        assertThat(linker.get(0).getDokumentId()).isEqualTo("463753696");
        assertThat(linker.get(0).getJournalpostId().getVerdi()).isEqualTo("448179511");
    }

}

