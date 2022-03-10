package no.nav.k9.sak.behandling.hendelse.produksjonsstyring;

import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.streams.StreamsConfig;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@ApplicationScoped
public class PubliserProduksjonsstyringHendelseProducer extends KafkaProducer {

    public PubliserProduksjonsstyringHendelseProducer() {
        // for CDI proxy
    }

    @Inject
    public PubliserProduksjonsstyringHendelseProducer(@KonfigVerdi("kafka.produksjonsstyring.topic") String topic,
                                                      @KonfigVerdi("KAFKA_BROKERS") String aivenBootstrapServers,
                                                      @KonfigVerdi("KAFKA_TRUSTSTORE_PATH") String aivenTruststorePath,
                                                      @KonfigVerdi("KAFKA_KEYSTORE_PATH") String aivenKeystorePath,
                                                      @KonfigVerdi("KAFKA_CREDSTORE_PASSWORD") String aivenCredstorePassword,
                                                      @KonfigVerdi(value = "KAFKA_OVERRIDE_KEYSTORE_PASSWORD", required = false) String overrideKeystorePassword,
                                                      @KonfigVerdi(value = "NAIS_NAMESPACE", defaultVerdi = "k9saksbehandling") String appNamespace,
                                                      @KonfigVerdi(value = "NAIS_APP_NAME", defaultVerdi = "k9-sak") String appName,
                                                      @KonfigVerdi("systembruker.username") String username,
                                                      @KonfigVerdi("systembruker.password") String password) {
        Properties properties = new Properties();

        properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, "KP-" + topic);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, appNamespace + "." + appName);
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, aivenBootstrapServers);
        if (overrideKeystorePassword != null) {
            setSecurity(username, properties);
            setUsernameAndPassword(username, password, properties);
        } else {
            properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

            properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
            properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, aivenTruststorePath);
            properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, aivenCredstorePassword);

            properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
            properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, aivenKeystorePath);
            properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, aivenCredstorePassword);
            properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, aivenCredstorePassword);
        }

        this.producer = createProducer(properties);
        this.topic = topic;
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, nøkkel, json));
    }
}
