package no.nav.ung.domenetjenester.personhendelser;

import org.apache.avro.Schema;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroJsonUtils {
    private static final Logger log = LoggerFactory.getLogger(AvroJsonUtils.class);

    public static String tilJson(SpecificRecord avroObject) {
        Schema schema = avroObject.getSchema();
        SpecificDatumWriter<Object> writer = new SpecificDatumWriter<>(schema);
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            Encoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, stream);
            writer.write(avroObject, jsonEncoder);
            jsonEncoder.flush();
            return stream.toString();
        } catch (IOException e) {
            log.error("Serialiseringsfeil:", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static SpecificRecord fraJson(String json, Schema schema) {
        DatumReader<SpecificRecord> reader = new SpecificDatumReader<>(schema);
        try {
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
            return reader.read(null, decoder);
        } catch (IOException e) {
            log.error("Deserialiseringsfeil: {} json: {}", e.getMessage(), json);
            throw new RuntimeException(e);
        }
    }
}
