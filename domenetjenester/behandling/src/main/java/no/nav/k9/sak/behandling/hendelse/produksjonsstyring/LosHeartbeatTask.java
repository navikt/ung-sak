package no.nav.k9.sak.behandling.hendelse.produksjonsstyring;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

/**
 * På Aiven i nais-plattformen mister consumer offset hvis det går over en uke mellom hver gang det commites offsets. Det igjen begyr
 * at offset vil bli mistet hvis det går en uke uten trafikk. Det vil sannsynligvis ikke skje, men har dårlige konsekvenser enn så lenge k9-los
 * ikke er idempotent ifht disse meldingene
 * <p>
 * Sender derfor her periodisk meldinger som leses, men kan av consumeren. Bare slik at consumeren for oppdatert offset.
 * <p>
 * Se https://doc.nais.io/persistence/kafka/offsets/
 */
@ApplicationScoped
@ProsessTask(value = "kafka.heartbeat.los", cronExpression = "0 0 12 * * * ")
public class LosHeartbeatTask implements ProsessTaskHandler {

    private ProsessEventKafkaProducer kafkaProducer;
    private boolean brukAiven;

    public LosHeartbeatTask() {
        //for CDI proxy
    }

    @Inject
    public LosHeartbeatTask(ProsessEventKafkaProducer kafkaProducer,
                            @KonfigVerdi(value = "ENABLE_PRODUCER_AKSJONSPUNKTHENDELSE_LOS_AIVEN", defaultVerdi = "false") boolean brukAiven) {
        this.kafkaProducer = kafkaProducer;
        this.brukAiven = brukAiven;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (brukAiven) {
            kafkaProducer.sendHeartbeat();
        }
    }
}
