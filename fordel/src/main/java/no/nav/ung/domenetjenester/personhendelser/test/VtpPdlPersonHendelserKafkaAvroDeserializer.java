package no.nav.ung.domenetjenester.personhendelser.test;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.person.pdl.leesah.Personhendelse;

import java.util.Map;

public class VtpPdlPersonHendelserKafkaAvroDeserializer extends KafkaAvroDeserializer {

    VtpPdlPersonHendelserKafkaAvroDeserializer() {
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
                return new AvroSchema(Personhendelse.SCHEMA$);
            }

            @Override
            public ParsedSchema getSchemaBySubjectAndId(String subject, int id) {
                return new AvroSchema(Personhendelse.SCHEMA$);
            }
        };
    }
}
