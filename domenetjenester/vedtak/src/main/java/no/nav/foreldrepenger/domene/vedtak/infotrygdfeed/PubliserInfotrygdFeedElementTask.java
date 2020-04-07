package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka.InfotrygdFeedMeldingProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@ProsessTask(PubliserInfotrygdFeedElementTask.TASKTYPE)
public class PubliserInfotrygdFeedElementTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.publiserInfotrygdFeedElement";
    public static final String KAFKA_KEY_PROPERTY = TASKTYPE + ".kafkaKey";

    private static final Logger logger = LoggerFactory.getLogger(PubliserInfotrygdFeedElementTask.class);

    private final InfotrygdFeedMeldingProducer meldingProducer;

    public PubliserInfotrygdFeedElementTask() {
        // CDI
        meldingProducer = null;
    }

    @Inject
    public PubliserInfotrygdFeedElementTask(InfotrygdFeedMeldingProducer meldingProducer) {
        this.meldingProducer = meldingProducer;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        String key = pd.getPropertyValue(KAFKA_KEY_PROPERTY);
        String value = pd.getPayloadAsString();

        logger.info("Publiserer hendelse til Infotrygd Feed. Key: '{}', Value: '{}'", key, value);

        meldingProducer.send(key, value);
    }
}
