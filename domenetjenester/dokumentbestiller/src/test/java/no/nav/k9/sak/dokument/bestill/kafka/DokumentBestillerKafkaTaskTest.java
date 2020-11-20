package no.nav.k9.sak.dokument.bestill.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.k9.formidling.kontrakt.dokumentdataparametre.DokumentdataParametreK9;
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling;
import no.nav.k9.formidling.kontrakt.kodeverk.AvsenderApplikasjon;
import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

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
        assertThat(dokumentbestilling.getDokumentMal()).isEqualTo(DokumentMalType.INNVILGELSE_DOK.getKode());
        assertThat(dokumentbestilling.getEksternReferanse()).isEqualTo(behandling.getUuid().toString());
        assertThat(dokumentbestilling.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(dokumentbestilling.getYtelseType().getKode()).isEqualTo("PSB");

        DokumentdataParametreK9 dokumentdata = JsonObjectMapper.OM.convertValue(dokumentbestilling.getDokumentdata(), DokumentdataParametreK9.class);
        assertThat(dokumentdata.getFritekst()).isEqualTo("en fritekst");
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
        try {
            prosessTaskData.setPayload(JsonObjectMapper.getJson("en fritekst"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
