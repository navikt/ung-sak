package no.nav.k9.sak.domene.typer.tid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public class JsonObjectMapper {

    public static final ObjectMapper OM;

    static {
        OM = new ObjectMapper();
        OM.registerModule(new JavaTimeModule());
        OM.registerModule(new Jdk8Module());
        OM.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OM.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OM.readerFor(clazz).readValue(json);
        } catch (IOException e) {
            throw JsonMapperFeil.FACTORY.ioExceptionVedLesing(e).toException();
        }
    }

    public static String getJson(Object object) throws IOException {
        Writer jsonWriter = new StringWriter();
        OM.writerWithDefaultPrettyPrinter().writeValue(jsonWriter, object);
        jsonWriter.flush();
        return jsonWriter.toString();
    }

    public String readKey(String data, String... keys) throws IOException {
        JsonNode jsonNode = OM.readTree(data);
        for (String key : keys) {
            if (jsonNode == null)
                break;
            jsonNode = jsonNode.get(key);
        }
        return jsonNode == null ? null : jsonNode.asText();
    }

    interface JsonMapperFeil extends DeklarerteFeil {
        JsonMapperFeil FACTORY = FeilFactory.create(JsonMapperFeil.class);

        @TekniskFeil(feilkode = "F-713321", feilmelding = "Fikk IO exception ved parsing av JSON", logLevel = LogLevel.WARN)
        Feil ioExceptionVedLesing(IOException cause);
    }
}
