package no.nav.ung.domenetjenester.arkiv.test;

import java.util.Map;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import no.nav.person.pdl.leesah.Personhendelse;

public class VtpKafkaAvroDeserializer extends KafkaAvroDeserializer {

    VtpKafkaAvroDeserializer() {
        super(getMockClient(), Map.of(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true",
                "schema.registry.url", "http://localhost:8081/mock"));
    }

    @Override
    public Object deserialize(String topic, byte[] bytes) {
        super.schemaRegistry = getMockClient();
        return super.deserialize(topic, bytes);
    }

    private static SchemaRegistryClient getMockClient() {
        return new MockSchemaRegistryClient() {
            @Override
            public synchronized AvroSchema getSchemaById(int id) {
                return new AvroSchema(Personhendelse.SCHEMA$);
            }

            @Override
            public ParsedSchema getSchemaBySubjectAndId(String subject, int id) {
                if (subject.contains("teamdokumenthandtering")) { // teamdokumenthandtering.aapen-dok-journalfoering-q1-value
                    return new AvroSchema(JournalfoeringHendelseRecord.SCHEMA$);
                }
                return new AvroSchema(Personhendelse.SCHEMA$);
            }
        };
    }
}
