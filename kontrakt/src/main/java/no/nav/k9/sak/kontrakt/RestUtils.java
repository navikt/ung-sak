package no.nav.k9.sak.kontrakt;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class RestUtils {

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
        return OM.copy();
    }
    
    public static String convertObjectToQueryStringFraMap(Map<String, String> queryParams) {
        var fmt = new UriFormat();
        queryParams.entrySet().forEach(e -> fmt.addToUri(e.getKey(), String.valueOf(e.getValue())));
        return fmt.toString();
    }
    
    public static String convertObjectToQueryString(Object object) {
        return OM.convertValue(object, UriFormat.class).toString();
    }
}
