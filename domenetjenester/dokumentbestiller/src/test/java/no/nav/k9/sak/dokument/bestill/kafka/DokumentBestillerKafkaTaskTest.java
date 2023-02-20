package no.nav.k9.sak.dokument.bestill.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.formidling.kontrakt.dokumentdataparametre.DokumentdataParametreK9;
import no.nav.k9.formidling.kontrakt.dokumentdataparametre.FritekstbrevinnholdDto;
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling;
import no.nav.k9.formidling.kontrakt.kodeverk.AvsenderApplikasjon;
import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class DokumentBestillerKafkaTaskTest {

    @Inject
    private EntityManager entityManager;

    private DokumentbestillingProducer dokumentbestillingProducer;
    private DokumentBestillerKafkaTask dokumentBestillerKafkaTask;
    private BehandlingRepositoryProvider repositoryProvider;


    @BeforeEach
    public void setup() {
        dokumentbestillingProducer = Mockito.mock(DokumentbestillingProducer.class);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

        dokumentBestillerKafkaTask = new DokumentBestillerKafkaTask(dokumentbestillingProducer, behandlingRepository);
    }

    @Test
    public void skal_mappe_riktige_felter_til_formidling() throws IOException {
        var behandling = lagBehandling();
        String bestillingUuid = UUID.randomUUID().toString();
        ArgumentCaptor<String> kafkaJson = ArgumentCaptor.forClass(String.class);

        var dokumentdataParams = new DokumentdataParametreK9();
        dokumentdataParams.setFritekst("en fritekst");

        String payload = JsonObjectMapper.getJson(dokumentdataParams);

        ProsessTaskData prosessTaskData = dokumentbestillingProsessTask(behandling, bestillingUuid, payload, DokumentMalType.INNVILGELSE_DOK);

        dokumentBestillerKafkaTask.doTask(prosessTaskData);

        Mockito.verify(dokumentbestillingProducer).publiserDokumentbestillingJson(kafkaJson.capture());
        Dokumentbestilling dokumentbestilling = JsonObjectMapper.fromJson(kafkaJson.getValue(), Dokumentbestilling.class);

        assertThat(dokumentbestilling.getAktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(dokumentbestilling.getAvsenderApplikasjon()).isEqualTo(AvsenderApplikasjon.K9SAK);
        assertThat(dokumentbestilling.getDokumentbestillingId()).isEqualTo(bestillingUuid);
        assertThat(dokumentbestilling.getDokumentMal()).isEqualTo(DokumentMalType.INNVILGELSE_DOK.getKode());
        assertThat(dokumentbestilling.getEksternReferanse()).isEqualTo(behandling.getUuid().toString());
        assertThat(dokumentbestilling.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(dokumentbestilling.getYtelseType().getKode()).isEqualTo("PSB");

        DokumentdataParametreK9 dokumentdata = JsonObjectMapper.OM.convertValue(dokumentbestilling.getDokumentdata(), DokumentdataParametreK9.class);
        assertThat(dokumentdata.getFritekst()).isEqualTo("en fritekst");
    }

    @Test
    public void skal_mappe_string_payload_som_fritekst_for_bakoverkompatiblitet() throws IOException {
        var behandling = lagBehandling();
        String bestillingUuid = UUID.randomUUID().toString();
        ArgumentCaptor<String> kafkaJson = ArgumentCaptor.forClass(String.class);

        String payload = JsonObjectMapper.getJson("en fritekst");
        ProsessTaskData prosessTaskData = dokumentbestillingProsessTask(behandling, bestillingUuid, payload, DokumentMalType.INNVILGELSE_DOK);

        dokumentBestillerKafkaTask.doTask(prosessTaskData);

        Mockito.verify(dokumentbestillingProducer).publiserDokumentbestillingJson(kafkaJson.capture());
        Dokumentbestilling dokumentbestilling = JsonObjectMapper.fromJson(kafkaJson.getValue(), Dokumentbestilling.class);
        DokumentdataParametreK9 dokumentdata = JsonObjectMapper.OM.convertValue(dokumentbestilling.getDokumentdata(), DokumentdataParametreK9.class);
        assertThat(dokumentdata.getFritekst()).isEqualTo("en fritekst");
    }

    @Test
    public void skal_mappe_fritekstbrev_til_formidling() throws IOException {
        var behandling = lagBehandling();
        String bestillingUuid = UUID.randomUUID().toString();
        ArgumentCaptor<String> kafkaJson = ArgumentCaptor.forClass(String.class);

        var dokumentdataParams = new DokumentdataParametreK9();
        FritekstbrevinnholdDto fritekstbrev = new FritekstbrevinnholdDto();
        fritekstbrev.setOverskrift("tittel");
        fritekstbrev.setBrødtekst("en fritekst");
        dokumentdataParams.setFritekstbrev(fritekstbrev);

        String payload = JsonObjectMapper.getJson(dokumentdataParams);
        ProsessTaskData prosessTaskData = dokumentbestillingProsessTask(behandling, bestillingUuid, payload, DokumentMalType.GENERELT_FRITEKSTBREV);

        dokumentBestillerKafkaTask.doTask(prosessTaskData);

        Mockito.verify(dokumentbestillingProducer).publiserDokumentbestillingJson(kafkaJson.capture());
        Dokumentbestilling dokumentbestilling = JsonObjectMapper.fromJson(kafkaJson.getValue(), Dokumentbestilling.class);

        assertThat(dokumentbestilling.getDokumentMal()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV.getKode());

        DokumentdataParametreK9 dokumentdata = JsonObjectMapper.OM.convertValue(dokumentbestilling.getDokumentdata(), DokumentdataParametreK9.class);
        assertThat(dokumentdata.getFritekstbrev().getBrødtekst()).isEqualTo("en fritekst");
        assertThat(dokumentdata.getFritekstbrev().getOverskrift()).isEqualTo("tittel");
    }

    @Test
    public void skal_mappe_OMP_til_OMSORGSPENGER() throws IOException {
        var behandling = lagOMP_behandling();
        ArgumentCaptor<String> kafkaJson = ArgumentCaptor.forClass(String.class);

        ProsessTaskData prosessTaskData = dokumentbestillingProsessTask(behandling, UUID.randomUUID().toString(), null, DokumentMalType.INNVILGELSE_DOK);

        dokumentBestillerKafkaTask.doTask(prosessTaskData);

        Mockito.verify(dokumentbestillingProducer).publiserDokumentbestillingJson(kafkaJson.capture());
        Dokumentbestilling dokumentbestilling = JsonObjectMapper.fromJson(kafkaJson.getValue(), Dokumentbestilling.class);

        assertThat(dokumentbestilling.getYtelseType()).isEqualTo(no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType.OMSORGSPENGER);

    }

    private ProsessTaskData dokumentbestillingProsessTask(Behandling behandling, String bestillingUuid, String payload, DokumentMalType dokumentMalType) {
        ProsessTaskData prosessTaskData =  ProsessTaskData.forProsessTask(DokumentBestillerKafkaTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BEHANDLING_ID, behandling.getId().toString());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.DOKUMENT_MAL_TYPE, dokumentMalType.getKode());
        prosessTaskData.setProperty(DokumentbestillerKafkaTaskProperties.BESTILLING_UUID, bestillingUuid);
        prosessTaskData.setPayload(payload);
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
