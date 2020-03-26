package no.nav.k9.sak.domene.abakus;

import java.io.IOException;

import org.threeten.extra.PeriodDuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class PeriodDurationDeserializer extends StdDeserializer<PeriodDuration> {

    public PeriodDurationDeserializer() {
        super((Class)null);
    }

    @Override
    public PeriodDuration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return PeriodDuration.parse(p.getText());
    }
}