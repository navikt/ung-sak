package no.nav.k9.sak.domene.abakus;

import java.io.IOException;

import org.threeten.extra.PeriodDuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class PeriodDurationSerializer extends StdSerializer<PeriodDuration> {

    public PeriodDurationSerializer() {
        super((Class)null);
    }

    @Override
    public void serialize(PeriodDuration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
    }
}