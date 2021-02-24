package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Json {

    private static final ObjectMapper OM;

    static {
        OM = new ObjectMapper();
        OM.registerModule(new Jdk8Module());
        OM.registerModule(new JavaTimeModule());
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OM.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ObjectMapper getObjectMapper() {
        return OM.copy();
    }
}
