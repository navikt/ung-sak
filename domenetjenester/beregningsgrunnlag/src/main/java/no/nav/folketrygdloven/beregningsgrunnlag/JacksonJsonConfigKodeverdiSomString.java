package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.kodeverk.TempKodeverdiSerializer;
import no.nav.k9.kodeverk.api.Kodeverdi;

public class JacksonJsonConfigKodeverdiSomString {

    private static final ObjectMapper OM = new ObjectMapper();

    static {
        OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OM.registerModule(new JavaTimeModule());

        var m = new SimpleModule();
        m.addSerializer(Kodeverdi.class, new TempKodeverdiSerializer());
        OM.registerModule(m);
    }

    private JacksonJsonConfigKodeverdiSomString() {
        // skjul public constructor
    }

    public static String toJson(Object object, Function<JsonProcessingException, Feil> feilFactory) {
        try {
            return OM.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw feilFactory.apply(e).toException();
        }
    }
}
