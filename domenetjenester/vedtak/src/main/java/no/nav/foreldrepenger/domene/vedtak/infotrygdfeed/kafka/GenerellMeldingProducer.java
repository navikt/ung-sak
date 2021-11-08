package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

// todo: Dette er copy/paste med små modifikasjoner fra modulen risikoklasifisering.
//       På tidspunktet hvor denne kommentaren ble skrevet så fantes det 6 versjoner av denne fra før, dette ble nr. 7.
//       Dette bør refactores til felles kode som kan deles mellom modulene.
public abstract class GenerellMeldingProducer {

    private Producer<String, String> producer;
    private String topic;

    protected GenerellMeldingProducer(String topic,
                            String bootstrapServers,
                            String schemaRegistryUrl,
                            String clientId,
                            String username,
                            String password) {
        Properties properties = new Properties();

        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("schema.registry.url", schemaRegistryUrl);
        properties.put("client.id", clientId);

        setSecurity(username, properties);
        setUsernameAndPassword(username, password, properties);

        this.producer = createProducer(properties);
        this.topic = topic;

    }

    protected GenerellMeldingProducer() {
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

    private void setUsernameAndPassword(String username, String password, Properties properties) {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, username, password);
            properties.put("sasl.jaas.config", jaasCfg);
        }
    }

    private Producer<String, String> createProducer(Properties properties) {
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    private void setSecurity(String username, Properties properties) {
        if (username != null && !username.isEmpty()) {
            properties.put("security.protocol", "SASL_SSL");
            properties.put("sasl.mechanism", "PLAIN");
        }
    }

    public void send(String key, String value) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, key, value));
    }

}
