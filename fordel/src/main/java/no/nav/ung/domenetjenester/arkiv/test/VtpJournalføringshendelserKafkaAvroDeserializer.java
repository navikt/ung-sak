package no.nav.ung.domenetjenester.arkiv.test;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import no.nav.ung.domenetjenester.personhendelser.utils.SchemaRegistryClientUtil;

import java.util.HashMap;
import java.util.Map;

public class VtpJournalføringshendelserKafkaAvroDeserializer extends KafkaAvroDeserializer {

    VtpJournalføringshendelserKafkaAvroDeserializer() {
        super(SchemaRegistryClientUtil.geSchemaRegistryMockClient());
    }

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        Map<String, ?> schemaRegistryConfig = SchemaRegistryClientUtil.getSchemaRegistryConfig();

        Map<String, Object> newProps = HashMap.newHashMap(props.size() + schemaRegistryConfig.size());

        newProps.putAll(props);
        newProps.putAll(schemaRegistryConfig);

        super.configure(newProps, isKey);
    }

    @Override
    public Object deserialize(String topic, byte[] bytes) {
        return super.deserialize(topic, bytes);
    }
}
