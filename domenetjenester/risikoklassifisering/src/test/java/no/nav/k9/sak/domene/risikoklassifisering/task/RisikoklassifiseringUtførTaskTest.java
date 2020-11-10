package no.nav.k9.sak.domene.risikoklassifisering.task;


import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import no.nav.k9.sak.domene.risikoklassifisering.RisikoklassifiseringUtførTask;
import no.nav.k9.sak.domene.risikoklassifisering.kafka.config.RisikoklassifiseringKafkaProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@RunWith(MockitoJUnitRunner.class)
public class RisikoklassifiseringUtførTaskTest {

    private static final Long BEHANDLING_ID = 123342L;

    private static final String TASKTYPE = "risiko.klassifisering";

    private static final String KONSUMENT_ID = "konsumentId";

    @Mock
    private RisikoklassifiseringKafkaProducer kafkaProducer;

    private RisikoklassifiseringUtførTask risikoklassifiseringUtførTask;

    @BeforeEach
    public void init(){
        MockitoAnnotations.initMocks(this);
        risikoklassifiseringUtførTask
            = new RisikoklassifiseringUtførTask(kafkaProducer);
    }

    @Test
    public void skal_produsere_melding_til_kafka(){
        ProsessTaskData prosessTaskData = new ProsessTaskData(TASKTYPE);
        prosessTaskData.setPayload("json");

        String konsumentId = UUID.randomUUID().toString();
        prosessTaskData.setProperty(KONSUMENT_ID, konsumentId);

        prosessTaskData.setProperty(ProsessTaskData.BEHANDLING_ID, String.valueOf(BEHANDLING_ID));
        risikoklassifiseringUtførTask.doTask(prosessTaskData);
        verify(kafkaProducer).publiserEvent(konsumentId, "json");
    }

}
