package no.nav.k9.sak.dokument.bestill.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling;
import no.nav.k9.formidling.kontrakt.kodeverk.AvsenderApplikasjon;
import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class DokumentBestillerKafkaTaskTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private DokumentbestillingProducer dokumentbestillingProducer;
    private DokumentBestillerKafkaTask dokumentBestillerKafkaTask;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;


    @Before
    public void setup() {
        dokumentbestillingProducer = Mockito.mock(DokumentbestillingProducer.class);
        repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());

        behandlingRepository = repositoryProvider.getBehandlingRepository();


        dokumentBestillerKafkaTask = new DokumentBestillerKafkaTask(dokumentbestillingProducer, behandlingRepository);
    }

    @Test
    public void skal_mappe_riktige_felter_til_formidling() {
        var behandling = lagBehandling();
        String bestillingUuid = UUID.randomUUID().toString();
        ArgumentCaptor<String> kafkaJson = ArgumentCaptor.forClass(String.class);

        ProsessTaskData prosessTaskData = dokumentbestillingProsessTask(behandling, bestillingUuid);

        dokumentBestillerKafkaTask.doTask(prosessTaskData);

        Mockito.verify(dokumentbestillingProducer).publiserDokumentbestillingJson(kafkaJson.capture());
        Dokumentbestilling dokumentbestilling = JsonObjectMapper.fromJson(kafkaJson.getValue(), Dokumentbestilling.class);

        assertThat(dokumentbestilling.getAktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(dokumentbestilling.getAvsenderApplikasjon()).isEqualTo(AvsenderApplikasjon.K9SAK);
        assertThat(dokumentbestilling.getDokumentbestillingId()).isEqualTo(bestillingUuid);
        assertThat(dokumentbestilling.getDokumentdata()).isNull();
        assertThat(dokumentbestilling.getDokumentMal()).isEqualTo(DokumentMalType.INNVILGELSE_DOK.getKode());
        assertThat(dokumentbestilling.getEksternReferanse()).isEqualTo(behandling.getUuid().toString());
        assertThat(dokumentbestilling.getFritekst()).isEqualTo("en fritekst");
        assertThat(dokumentbestilling.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(dokumentbestilling.getYtelseType().getKode()).isEqualTo("PSB");

        assertThat(dokumentbestilling.getArsakskode()).isNull();
        assertThat(dokumentbestilling.getBehandlendeEnhetNavn()).isNull();
        assertThat(dokumentbestilling.getBehandlingUuid()).isNull();
        assertThat(dokumentbestilling.getDokumentbestillingUuid()).isNull();
        assertThat(dokumentbestilling.getHistorikkAktør()).isNull();


    }

    @Test
    public void skal_mappe_OMP_til_OMSORGSPENGER() {
        var behandling = lagOMP_behandling();
        ArgumentCaptor<String> kafkaJson = ArgumentCaptor.forClass(String.class);

        ProsessTaskData prosessTaskData = dokumentbestillingProsessTask(behandling, UUID.randomUUID().toString());

        dokumentBestillerKafkaTask.doTask(prosessTaskData);

        Mockito.verify(dokumentbestillingProducer).publiserDokumentbestillingJson(kafkaJson.capture());
        Dokumentbestilling dokumentbestilling = JsonObjectMapper.fromJson(kafkaJson.getValue(), Dokumentbestilling.class);

        assertThat(dokumentbestilling.getYtelseType()).isEqualTo(no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType.OMSORGSPENGER);

    }

    private ProsessTaskData dokumentbestillingProsessTask(Behandling behandling, String bestillingUuid) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(DokumentbestillerKafkaTaskProperties.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID, behandling.getId().toString());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE, DokumentMalType.INNVILGELSE_DOK.getKode());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BESTILLING_UUID, bestillingUuid);
        prosessTaskData.setPayload("\"en fritekst\"");
        return prosessTaskData;
    }

    private Behandling lagOMP_behandling() {
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMSORGSPENGER);
        scenario.lagre(repositoryProvider);
        return scenario.getBehandling();
    }

    private Behandling lagBehandling() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.lagre(repositoryProvider);
        return scenario.getBehandling();
    }

}
