package no.nav.k9.sak.dokument.bestill.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.dokument.bestill.BrevHistorikkinnslag;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DokumentKafkaBestillerTest {

    @Inject
    private EntityManager entityManager;

    private DokumentKafkaBestiller dokumentKafkaBestiller;
    private BehandlingRepositoryProvider repositoryProvider;

    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    @Mock
    private BrevHistorikkinnslag brevHistorikkinnslag;

    private Behandling behandling;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        ProsessTaskEventPubliserer eventPubliserer = Mockito.mock(ProsessTaskEventPubliserer.class);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, eventPubliserer);

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.lagre(repositoryProvider);
        behandling = scenario.getBehandling();
        dokumentKafkaBestiller = new DokumentKafkaBestiller(
            behandlingRepository,
            prosessTaskRepository,
            brevHistorikkinnslag);
    }

    @Test
    public void skal_opprette_historikkinnslag_og_lagre_prosesstask() {
        var innhentDok = DokumentMalType.INNHENT_DOK;
        BestillBrevDto bestillBrevDto = lagBestillBrevDto(innhentDok, null);
        HistorikkAktør aktør = HistorikkAktør.SAKSBEHANDLER;
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
        Mockito.verify(brevHistorikkinnslag, Mockito.times(1)).opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, innhentDok);
        List<ProsessTaskData> prosessTaskDataListe = prosessTaskRepository.finnIkkeStartet();
        assertThat(prosessTaskDataListe).anySatisfy(taskData -> {
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID)).isEqualTo(behandling.getId().toString());
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE)).isEqualTo(innhentDok.getKode());
            assertThat(JsonObjectMapper.fromJson(taskData.getPayloadAsString(), String.class)).isNull();
        });
    }

    @Test
    public void skal_opprette_historikkinnslag_og_lagre_prosesstask_med_fritekst() {
        var innhentDok = DokumentMalType.INNHENT_DOK;
        String fritekst = "FRITEKST";
        BestillBrevDto bestillBrevDto = lagBestillBrevDto(innhentDok, fritekst);
        HistorikkAktør aktør = HistorikkAktør.SAKSBEHANDLER;
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
        Mockito.verify(brevHistorikkinnslag, Mockito.times(1)).opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, innhentDok);
        List<ProsessTaskData> prosessTaskDataListe = prosessTaskRepository.finnIkkeStartet();
        assertThat(prosessTaskDataListe).anySatisfy(taskData -> {
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID)).isEqualTo(behandling.getId().toString());
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE)).isEqualTo(innhentDok.getKode());
            assertThat(JsonObjectMapper.fromJson(taskData.getPayloadAsString(), String.class)).isEqualTo(fritekst);
        });
    }

    private BestillBrevDto lagBestillBrevDto(DokumentMalType dokumentMalType, String fritekst) {
        return new BestillBrevDto(behandling.getId(), no.nav.k9.kodeverk.dokument.DokumentMalType.fraKode(dokumentMalType.getKode()), fritekst, null);
    }

}
