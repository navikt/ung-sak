package no.nav.ung.sak.oppgave.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.apptjeneste.AppServiceHandler;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class OppgaveBekreftelseConsumer implements AppServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(OppgaveBekreftelseConsumer.class);
    private KafkaStreams stream;
    private String topic;
    private boolean consumerEnabled;

    OppgaveBekreftelseConsumer() {
    }

    @Inject
    public OppgaveBekreftelseConsumer(OppgaveBekreftelseHendelseHåndterer oppgaveBekreftelseHendelseHåndterer,
                                      OppgaveBekreftelseStreamKafkaProperties streamKafkaProperties,
                                      @KonfigVerdi(value = "OPPGAVER_I_UNGSAK_ENABLED", defaultVerdi = "true") boolean consumerEnabled) {
        this.topic = streamKafkaProperties.getTopic();
        this.consumerEnabled = consumerEnabled;

        if(!consumerEnabled) {
            return;
        }

        Consumed<String, String> consumed = Consumed.with(Topology.AutoOffsetReset.EARLIEST);

        var builder = new StreamsBuilder();
        builder.stream(this.topic, consumed)
            .foreach(oppgaveBekreftelseHendelseHåndterer::handleMessage);
        var topology = builder.build();

        this.stream = new KafkaStreams(topology, streamKafkaProperties.setupProperties());
    }

    private void addShutdownHooks() {
        stream.setStateListener((newState, oldState) -> {
            log.info("{} :: From state={} to state={}", topic, oldState, newState);

            if (newState == KafkaStreams.State.ERROR) {
                // if the stream has died there is no reason to keep spinning
                log.error("{} :: No reason to keep living, closing stream", topic);
                stop();
            }
        });

        stream.setUncaughtExceptionHandler(throwable -> {
            log.error("{} :: Stream died with exception", topic, throwable);
            try {
                // Her kan vi hamne når f.eks. brokers er nede, node-oppgraderinger, credentials som ikke roteres i tid etc.
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
        });
    }

    @Override
    public void start() {
        if (consumerEnabled) {
            addShutdownHooks();
            stream.start();
            log.info("Starter konsumering av topic={}, tilstand={}", topic, stream.state());
        }
    }

    @Override
    public void stop() {
        if (consumerEnabled) {
            log.info("Starter shutdown av topic={}, tilstand={} med 10 sekunder timeout", topic, stream.state());
            stream.close(Duration.of(60, ChronoUnit.SECONDS));
            log.info("Shutdown av topic={}, tilstand={} med 10 sekunder timeout", topic, stream.state());
        }
    }
}
