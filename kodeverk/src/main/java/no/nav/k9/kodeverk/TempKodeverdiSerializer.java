package no.nav.k9.kodeverk;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.k9.kodeverk.api.Kodeverdi;

/**
 * For migrering til kodeverk på nytt format kan denne brukes for å override serialisering av Kodeverdi til object.
 * Denne vil da serialisere til String. Må legges på objectmapper der det er ønskelig
 */
public class TempKodeverdiSerializer extends StdSerializer<Kodeverdi> {

    public TempKodeverdiSerializer(){
        super(Kodeverdi.class);
    }

    @Override
    public void serialize(Kodeverdi value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getKode());
    }
}
