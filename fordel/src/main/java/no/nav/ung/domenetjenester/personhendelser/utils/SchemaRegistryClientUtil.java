package no.nav.ung.domenetjenester.personhendelser.utils;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import no.nav.person.pdl.leesah.Personhendelse;

import java.util.Map;

public class SchemaRegistryClientUtil {

    public static MockSchemaRegistryClient schemaRegistryMockClient = SchemaRegistryClientUtil.configureMockClient();
    public static final String MOCK_SCHEMA_REGISTRY_URL = "mock://dummy";

    public static Map<String, ?> getSchemaRegistryConfig() {
        return Map.of(
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true",
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SchemaRegistryClientUtil.MOCK_SCHEMA_REGISTRY_URL
        );
    }

    public static MockSchemaRegistryClient geSchemaRegistryMockClient() {
        return schemaRegistryMockClient;
    }

    private static MockSchemaRegistryClient configureMockClient() {
        return new MockSchemaRegistryClient() {

            @Override
            public ParsedSchema getSchemaBySubjectAndId(String subject, int id) {
                if (subject.contains("leesah")) return new AvroSchema(Personhendelse.SCHEMA$);
                else if (subject.contains("journalfoering"))
                    return new AvroSchema(JournalfoeringHendelseRecord.SCHEMA$);

                throw new IllegalArgumentException("Ikke støttet schema. Subject: " + subject + ", id: " + id);
            }
        };
    }
}
