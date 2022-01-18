package no.nav.k9.sak.dokument.bestill.kafka;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.serialization.StringSerializer;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@ApplicationScoped
public class DokumentbestillingProducer {

    Producer<String, String> producer;
    String topic;

    public DokumentbestillingProducer() {
        // for CDI proxy
    }

    @Inject
    public DokumentbestillingProducer(@KonfigVerdi("kafka.dokumentbestilling.topic") String topic,
                                      @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                      @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                      @KonfigVerdi("systembruker.username") String username,
                                      @KonfigVerdi("systembruker.password") String password) {
        Properties properties = new Properties();

        String clientId = "KP-" + topic;
        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("schema.registry.url", schemaRegistryUrl);
        properties.setProperty("client.id", clientId);

        setSecurity(username, properties);
        setUsernameAndPassword(username, password, properties);

        this.producer = createProducer(properties);
        this.topic = topic;

    }

    public void flush() {
        producer.flush();
    }

    void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            @SuppressWarnings("unused")
            var recordMetadata = producer.send(record).get(); // NOSONAR
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw DokumentbestillerKafkaFeil.FACTORY.uventetFeil(topic, e).toException();
        } catch (ExecutionException e) {
            throw DokumentbestillerKafkaFeil.FACTORY.uventetFeil(topic, e).toException();
        } catch (AuthenticationException | AuthorizationException e) {
            throw DokumentbestillerKafkaFeil.FACTORY.feilIPålogging(topic, e).toException();
        } catch (RetriableException e) {
            throw DokumentbestillerKafkaFeil.FACTORY.retriableExceptionMotKaka(topic, e).toException();
        } catch (KafkaException e) {
            throw DokumentbestillerKafkaFeil.FACTORY.annenExceptionMotKafka(topic, e).toException();
        }
    }

    void setUsernameAndPassword(String username, String password, Properties properties) {
        if ((username != null && !username.isEmpty())
            && (password != null && !password.isEmpty())) {
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, username, password);
            properties.setProperty("sasl.jaas.config", jaasCfg);
        }
    }

    Producer<String, String> createProducer(Properties properties) {
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    void setSecurity(String username, Properties properties) {
        if (username != null && !username.isEmpty()) {
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty("sasl.mechanism", "PLAIN");
        }
    }

    public void publiserDokumentbestillingJson(String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, json));
    }
}
