package no.nav.ung.kodeverk;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.ung.kodeverk.api.Kodeverdi;
import java.io.IOException;

/**
 * For default (legacy) object mapper overstyrer denne serialisering av Kodeverdier slik at den fortsetter å vere kompatibel
 * med det som har vore vanleg, nemleg å serialisere Kodeverdi enums til objekt, sjølv om vi fjerne @JsonFormat annotasjon
 * på Kodeverdi enums.
 */
public class LegacyKodeverdiSomObjektSerializer extends StdSerializer<Kodeverdi> {

    public LegacyKodeverdiSomObjektSerializer() {
        super(Kodeverdi.class);
    }

    @Override
    public void serialize(final Kodeverdi value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        final Class<?> cls = value.getClass();
        final LegacyKodeverdiJsonValue legacyKodeverdiJsonValue = cls.getAnnotation(LegacyKodeverdiJsonValue.class);
        if(legacyKodeverdiJsonValue != null) {
            // Kodeverdi enum har legacy serialisering til kode string. Spesialbehandler den her:
            gen.writeString(value.getKode());

        } else {
            gen.writeStartObject();
            gen.writeStringField("kode", value.getKode());
            if (value.getNavn() != null) {
                gen.writeStringField("navn", value.getNavn());
            }
            gen.writeStringField("kodeverk", value.getKodeverk());
            gen.writeEndObject();
        }
    }
}
