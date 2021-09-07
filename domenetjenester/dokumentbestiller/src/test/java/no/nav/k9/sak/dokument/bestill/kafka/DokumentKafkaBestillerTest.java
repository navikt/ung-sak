package no.nav.k9.sak.dokument.bestill.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.formidling.kontrakt.dokumentdataparametre.DokumentdataParametreK9;
import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.dokument.bestill.BrevHistorikkinnslag;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.dokument.FritekstbrevinnholdDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class DokumentKafkaBestillerTest {

    @Inject
    private EntityManager entityManager;
    private DokumentKafkaBestiller dokumentKafkaBestiller;
    private BehandlingRepositoryProvider repositoryProvider;

    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    private BrevHistorikkinnslag brevHistorikkinnslag;

    private Behandling behandling;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        brevHistorikkinnslag = Mockito.mock(BrevHistorikkinnslag.class);
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
        BestillBrevDto bestillBrevDto = lagBestillBrevDto(innhentDok, null, null);
        HistorikkAktør aktør = HistorikkAktør.SAKSBEHANDLER;
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
        Mockito.verify(brevHistorikkinnslag, Mockito.times(1)).opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, innhentDok);
        List<ProsessTaskData> prosessTaskDataListe = prosessTaskRepository.finnIkkeStartet();
        assertThat(prosessTaskDataListe).anySatisfy(taskData -> {
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID)).isEqualTo(behandling.getId().toString());
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE)).isEqualTo(innhentDok.getKode());
            assertThat(JsonObjectMapper.fromJson(taskData.getPayloadAsString(), DokumentdataParametreK9.class).getFritekst()).isNull();
        });
    }

    @Test
    public void skal_opprette_historikkinnslag_og_lagre_prosesstask_med_fritekst() {
        var innhentDok = DokumentMalType.INNHENT_DOK;
        String fritekst = "FRITEKST";
        BestillBrevDto bestillBrevDto = lagBestillBrevDto(innhentDok, fritekst, null);
        HistorikkAktør aktør = HistorikkAktør.SAKSBEHANDLER;
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
        Mockito.verify(brevHistorikkinnslag, Mockito.times(1)).opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, innhentDok);
        List<ProsessTaskData> prosessTaskDataListe = prosessTaskRepository.finnIkkeStartet();
        assertThat(prosessTaskDataListe).anySatisfy(taskData -> {
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID)).isEqualTo(behandling.getId().toString());
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE)).isEqualTo(innhentDok.getKode());
            assertThat(JsonObjectMapper.fromJson(taskData.getPayloadAsString(), DokumentdataParametreK9.class).getFritekst()).isEqualTo(fritekst);
        });
    }

    @Test
    public void skal_lagre_prosesstask_med_fritekst_brev() {
        var generellFritekstbrev = DokumentMalType.GENERELT_FRITEKSTBREV;
        String brødtekst = "FRITEKST";
        String tittel = "EN TITTEL";
        BestillBrevDto bestillBrevDto = lagBestillBrevDto(generellFritekstbrev, null, new FritekstbrevinnholdDto(tittel, brødtekst));
        HistorikkAktør aktør = HistorikkAktør.SAKSBEHANDLER;
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
        Mockito.verify(brevHistorikkinnslag, Mockito.times(1)).opprettHistorikkinnslagForBestiltBrevFraKafka(aktør, behandling, generellFritekstbrev);
        List<ProsessTaskData> prosessTaskDataListe = prosessTaskRepository.finnIkkeStartet();
        assertThat(prosessTaskDataListe).anySatisfy(taskData -> {
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID)).isEqualTo(behandling.getId().toString());
            assertThat(taskData.getPropertyValue(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE)).isEqualTo(generellFritekstbrev.getKode());
            no.nav.k9.formidling.kontrakt.dokumentdataparametre.FritekstbrevinnholdDto fritekstbrevinnhold = JsonObjectMapper.fromJson(taskData.getPayloadAsString(), DokumentdataParametreK9.class).getFritekstbrev();
            assertThat(fritekstbrevinnhold.getBrødtekst()).isEqualTo(brødtekst);
            assertThat(fritekstbrevinnhold.getOverskrift()).isEqualTo(tittel);
        });
    }

    private BestillBrevDto lagBestillBrevDto(DokumentMalType dokumentMalType, String fritekst, FritekstbrevinnholdDto fritekstbrev) {
        return new BestillBrevDto(behandling.getId(), no.nav.k9.kodeverk.dokument.DokumentMalType.fraKode(dokumentMalType.getKode()), fritekst, null, fritekstbrev);
    }

}
