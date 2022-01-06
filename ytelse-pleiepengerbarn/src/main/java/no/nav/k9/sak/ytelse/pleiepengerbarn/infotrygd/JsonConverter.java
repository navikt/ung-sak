package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygd;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class JsonConverter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    SakResponse sakResponse(String json) {
        return convert(json, new TypeReference<>() {
        });
    }

    List<VedtakPleietrengende> vedtakBarnResponse(String json) {
        return convert(json, new TypeReference<>() {
        });
    }

    List<PårørendeSykdom> grunnlagBarnResponse(String json) {
        return convert(json, new TypeReference<>() {
        });
    }


    private <T> T convert(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("Feil ved konvertering fra JSON", e);
            throw new InfotrygdPårørendeSykdomException("Feil ved konvertering fra JSON", e);
        }
    }
}
