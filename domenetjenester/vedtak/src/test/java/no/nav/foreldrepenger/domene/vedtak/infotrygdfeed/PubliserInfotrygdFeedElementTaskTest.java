package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka.InfotrygdFeedMeldingProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class PubliserInfotrygdFeedElementTaskTest {

    @Mock
    private InfotrygdFeedMeldingProducer meldingProducer;

    private PubliserInfotrygdFeedElementTask task;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        task = new PubliserInfotrygdFeedElementTask(meldingProducer);
    }

    @Test
    public void name() {
        ProsessTaskData pd = new ProsessTaskData(PubliserInfotrygdFeedElementTask.TASKTYPE);
        pd.setProperty(PubliserInfotrygdFeedElementTask.KAFKA_KEY_PROPERTY, "kafka-key");
        pd.setPayload("payload");

        task.doTask(pd);

        verify(meldingProducer).send("kafka-key", "payload");
    }
}
