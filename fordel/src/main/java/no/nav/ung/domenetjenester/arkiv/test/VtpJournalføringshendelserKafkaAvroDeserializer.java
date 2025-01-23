package no.nav.ung.domenetjenester.arkiv.test;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;

import java.util.Map;

public class VtpJournalføringshendelserKafkaAvroDeserializer extends KafkaAvroDeserializer {

    VtpJournalføringshendelserKafkaAvroDeserializer() {
        super(getMockClient(), Map.of(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true",
            "schema.registry.url", "mock://dummy"));
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
                return new AvroSchema(JournalfoeringHendelseRecord.SCHEMA$);
            }

            @Override
            public ParsedSchema getSchemaBySubjectAndId(String subject, int id) {
                return new AvroSchema(JournalfoeringHendelseRecord.SCHEMA$);
            }
        };
    }
}
