package no.nav.ung.sak.domene.typer.tid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import no.nav.k9.felles.feil.Feil;
import no.nav.ung.kodeverk.KodeverdiSomStringSerializer;
import no.nav.ung.kodeverk.api.Kodeverdi;

public class JsonObjectMapperKodeverdiSomStringSerializer {

    private static final ObjectMapper OM;

    static {
        OM = JsonObjectMapper.OM.copy();
        var m = new SimpleModule();
        m.addSerializer(Kodeverdi.class, new KodeverdiSomStringSerializer());
        OM.registerModule(m);
    }

    public static String getJson(Object object) throws IOException {
        Writer jsonWriter = new StringWriter();
        OM.writerWithDefaultPrettyPrinter().writeValue(jsonWriter, object);
        jsonWriter.flush();
        return jsonWriter.toString();
    }

    public static String toJson(Object object, Function<JsonProcessingException, Feil> feilFactory) {
        try {
            return OM.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw feilFactory.apply(e).toException();
        }
    }

}
