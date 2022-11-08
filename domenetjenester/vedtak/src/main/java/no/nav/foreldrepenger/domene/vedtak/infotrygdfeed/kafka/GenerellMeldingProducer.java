package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka;

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
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

// todo: Dette er copy/paste med små modifikasjoner fra modulen risikoklasifisering.
//       På tidspunktet hvor denne kommentaren ble skrevet så fantes det 6 versjoner av denne fra før, dette ble nr. 7.
//       Dette bør refactores til felles kode som kan deles mellom modulene.
public abstract class GenerellMeldingProducer {

    private Producer<String, String> producer;
    private String topic;

    GenerellMeldingProducer(String topic,
                            String bootstrapServers,
                            String clientId,
                            String truststorePath,
                            String truststorePassword,
                            String keystorePath,
                            String keystorePassword) {

        this.topic = topic;

        Properties properties = new Properties();

        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("client.id", clientId);

        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Security
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
        properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks");
        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
        properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
        properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystorePath);
        properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);

        this.producer = new KafkaProducer<String, String>(properties);
    }

    GenerellMeldingProducer() {
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
            throw InfotrygdKafkaProducerFeil.FACTORY.uventetFeil(topic, e).toException();
        } catch (AuthenticationException | AuthorizationException e) {
            throw InfotrygdKafkaProducerFeil.FACTORY.feilIPålogging(topic, e).toException();
        } catch (RetriableException e) {
            throw InfotrygdKafkaProducerFeil.FACTORY.retriableExceptionMotKaka(topic, e).toException();
        } catch (KafkaException e) {
            throw InfotrygdKafkaProducerFeil.FACTORY.annenExceptionMotKafka(topic, e).toException();
        }
    }

    public void send(String key, String value) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, key, value));
    }

}
