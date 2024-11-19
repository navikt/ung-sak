package no.nav.ung.kodeverk.geografisk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import no.nav.ung.kodeverk.KodeverdiSomStringSerializer;
import no.nav.ung.kodeverk.api.Kodeverdi;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Test at serialisering og deserialisering fungerer både som objekt og string
public class SpråkkodeTest {
    @Test
    public void testStringSerialiseringOgDeserialisering() throws JsonProcessingException {
        final ObjectMapper om = new ObjectMapper();
        var m = new SimpleModule();
        m.addSerializer(Kodeverdi.class, new KodeverdiSomStringSerializer());
        om.registerModule(m);

        final Språkkode original = Språkkode.en;
        final String serialized = om.writeValueAsString(original);
        assertThat(serialized).isEqualToIgnoringWhitespace("\"EN\"");
        final Språkkode deserialized = om.readValue(serialized, Språkkode.class);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    public void testObjektSerialiseringOgDeserialisering() throws JsonProcessingException {
        final ObjectMapper om = new ObjectMapper();

        final Språkkode original = Språkkode.en;
        final String serialized = om.writeValueAsString(original);
        assertThat(serialized).isEqualToIgnoringWhitespace("{\"kode\":\"EN\",\"kodeverk\":\"SPRAAK_KODE\"}");
        final Språkkode deserialized = om.readValue(serialized, Språkkode.class);
        assertThat(deserialized).isEqualTo(original);
    }
}
