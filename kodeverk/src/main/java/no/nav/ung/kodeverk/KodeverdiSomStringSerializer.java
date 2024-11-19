package no.nav.ung.kodeverk;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.io.IOException;

/**
 * For migrering til kodeverk på nytt format kan denne brukes for å override serialisering av Kodeverdi til object.
 * Denne vil da serialisere til String. Må legges på objectmapper der det er ønskelig
 */
public class KodeverdiSomStringSerializer extends StdSerializer<Kodeverdi> {

    public KodeverdiSomStringSerializer(){
        super(Kodeverdi.class);
    }

    @Override
    public void serialize(Kodeverdi value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getKode());
    }
}
