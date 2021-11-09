package no.nav.k9.sak.hendelse.stønadstatistikk;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkHendelse;

public class StønadstatistikkSerializer {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
    
    public static String toJson(StønadstatistikkHendelse object) {
        try {
            Writer jsonWriter = new StringWriter();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonWriter, object);
            jsonWriter.flush();
            return jsonWriter.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
