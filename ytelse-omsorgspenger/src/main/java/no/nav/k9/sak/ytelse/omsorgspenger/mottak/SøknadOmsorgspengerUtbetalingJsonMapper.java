package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues.Std;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.abakus.iaygrunnlag.kodeverk.KodeValidator;

public class SøknadOmsorgspengerUtbetalingJsonMapper {
    private static final ObjectMapper OM;

    public SøknadOmsorgspengerUtbetalingJsonMapper() {
    }

    public static ObjectMapper getMapper() {
        return OM;
    }

    public static ObjectMapper getMapper(KodeValidator validator) {
        Std std = new Std();
        std.addValue(KodeValidator.class, validator);
        ObjectMapper objectMapper = OM.copy();
        objectMapper.setInjectableValues(std);
        return objectMapper;
    }

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        objectMapper.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);
        Std std = new Std();
        std.addValue(KodeValidator.class, KodeValidator.HAPPY_VALIDATOR);
        objectMapper.setInjectableValues(std);
        OM = objectMapper;
    }
}
