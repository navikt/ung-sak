package no.nav.k9.sak.web.app.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class KalkulusKodelisteSerializer extends StdSerializer<no.nav.folketrygdloven.kalkulus.kodeverk.Kodeverdi> {

    public static final String KODE = "kode";
    public static final String KODEVERK = "kodeverk";

    public KalkulusKodelisteSerializer() {
        super(no.nav.folketrygdloven.kalkulus.kodeverk.Kodeverdi.class);
    }

    @Override
    public void serialize(no.nav.folketrygdloven.kalkulus.kodeverk.Kodeverdi value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        jgen.writeStartObject();

        jgen.writeStringField(KODE, value.getKode());

        jgen.writeStringField(KODEVERK, value.getKodeverk());

        jgen.writeEndObject();
    }


}
