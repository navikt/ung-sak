package no.nav.ung.kontrakt;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {

    private static final ObjectMapper OM;

    static {
        OM = new ObjectMapper();
        OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OM.registerModule(new JavaTimeModule());
    }

    public static ObjectMapper getObjectMapper() {
        return OM;
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws IOException{
        return OM.readerFor(clazz).readValue(json);
    }

    public static String getJson(Object object) throws IOException {
        Writer jsonWriter = new StringWriter();
        OM.writerWithDefaultPrettyPrinter().writeValue(jsonWriter, object);
        jsonWriter.flush();
        return jsonWriter.toString();
    }
}
