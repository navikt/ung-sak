package no.nav.ung.sak.hendelse.stønadstatistikk;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.IntegrasjonFeil;
import no.nav.k9.felles.feil.deklarasjon.ManglerTilgangFeil;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@ApplicationScoped
public class StønadstatistikkHendelseMeldingProducer {

    private Producer<String, String> producer;
    private String topic;

    public StønadstatistikkHendelseMeldingProducer() {
        // for CDI proxy
    }

    @Inject
    public StønadstatistikkHendelseMeldingProducer(
            @KonfigVerdi(value = "kafka.stonadstatistikk.topic", defaultVerdi = "k9saksbehandling.aapen-ung-stonadstatistikk-v1") String topic,
            @KonfigVerdi("KAFKA_BROKERS") String aivenBootstrapServers,
            @KonfigVerdi("KAFKA_TRUSTSTORE_PATH") String aivenTruststorePath,
            @KonfigVerdi("KAFKA_KEYSTORE_PATH") String aivenKeystorePath,
            @KonfigVerdi("KAFKA_CREDSTORE_PASSWORD") String aivenCredstorePassword,
            @KonfigVerdi(value = "KAFKA_OVERRIDE_KEYSTORE_PASSWORD", required = false) String overrideKeystorePassword,
            @KonfigVerdi(value = "NAIS_NAMESPACE", defaultVerdi = "k9saksbehandling") String appNamespace,
            @KonfigVerdi(value = "NAIS_APP_NAME", defaultVerdi = "ung-sak") String appName,
            @KonfigVerdi("systembruker.username") String username,
            @KonfigVerdi("systembruker.password") String password
            ) {
        Properties properties = new Properties();
        properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, "KP-" + topic);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, appNamespace + "." + appName);
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, aivenBootstrapServers);
        properties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "5242880"); // 5MB

        if (overrideKeystorePassword != null) {
            // TODO: Gjør at dette er mulig mot vtp:
            //properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, overrideKeystorePassword);
            //properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, overrideKeystorePassword);
            // ...og fjern da disse:
            setSecurity(username, properties);
            setUsernameAndPassword(username, password, properties);
        } else {
            properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
            properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
            properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
            properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, aivenTruststorePath);
            properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, aivenCredstorePassword);
            properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, aivenKeystorePath);
            properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, aivenCredstorePassword);
            properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, aivenCredstorePassword);
        }

        this.producer = createProducer(properties);
        this.topic = topic;
    }

    public void flushAndClose() {
        producer.flush();
        producer.close();
    }

    public void flush() {
        producer.flush();
    }

    private void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            producer.send(record)
                .get();
        } catch (InterruptedException | ExecutionException e) {
            throw KafkaProducerFeil.FACTORY.uventetFeil(topic, e).toException();
        } catch (AuthenticationException | AuthorizationException e) {
            throw KafkaProducerFeil.FACTORY.feilIPålogging(topic, e).toException();
        } catch (RetriableException e) {
            throw KafkaProducerFeil.FACTORY.retriableExceptionMotKaka(topic, e).toException();
        } catch (KafkaException e) {
            throw KafkaProducerFeil.FACTORY.annenExceptionMotKafka(topic, e).toException();
        }
    }

    private Producer<String, String> createProducer(Properties properties) {
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    public void send(String key, String value) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, key, value));
    }

    private void setUsernameAndPassword(String username, String password, Properties properties) {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, username, password);
            properties.put("sasl.jaas.config", jaasCfg);
        }
    }

    private void setSecurity(String username, Properties properties) {
        if (username != null && !username.isEmpty()) {
            properties.put("security.protocol", "SASL_SSL");
            properties.put("sasl.mechanism", "PLAIN");
        }
    }

    private interface KafkaProducerFeil extends DeklarerteFeil {

        KafkaProducerFeil FACTORY = FeilFactory.create(KafkaProducerFeil.class);

        // todo: fyll ut riktige koder...
        @ManglerTilgangFeil(feilkode = "K9-FEED-821005", feilmelding = "Feil i pålogging mot Kafka, topic:%s", logLevel = LogLevel.ERROR)
        Feil feilIPålogging(String topic, Exception e);

        @IntegrasjonFeil(feilkode = "K9-FEED-925469", feilmelding = "Uventet feil ved sending til Kafka, topic:%s", logLevel = LogLevel.WARN)
        Feil uventetFeil(String topic, Exception e);

        @IntegrasjonFeil(feilkode = "K9-FEED-127608", feilmelding = "Fikk transient feil mot Kafka, kan prøve igjen, topic:%s", logLevel = LogLevel.WARN)
        Feil retriableExceptionMotKaka(String topic, RetriableException e);

        @IntegrasjonFeil(feilkode = "K9-FEED-811208", feilmelding = "Fikk feil mot Kafka, topic:%s", logLevel = LogLevel.WARN)
        Feil annenExceptionMotKafka(String topic, KafkaException e);


    }


}
