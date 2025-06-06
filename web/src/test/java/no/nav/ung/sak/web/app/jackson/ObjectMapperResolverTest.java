package no.nav.ung.sak.web.app.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectMapperResolverTest {

    final ObjectMapperResolver resolver = new ObjectMapperResolver();
    final ObjectMapper defaultMapper = resolver.getDefaultObjectMapper();
    final ObjectMapper openapiMapper = resolver.getOpenapiObjektMapper();

    @Test
    public void testSpråkkodeDeSer() throws JsonProcessingException {
        final Språkkode original = Språkkode.en;
        final String defaultSerialized = defaultMapper.writeValueAsString(original);
        assertThat(defaultSerialized).isEqualToIgnoringWhitespace("{\"kode\":\"EN\", \"kodeverk\":\"SPRAAK_KODE\"}");
        final String openapiSerialized = openapiMapper.writeValueAsString(original);
        assertThat(openapiSerialized).isEqualToIgnoringWhitespace("\"EN\"");
        final Språkkode defaultDeserialized = defaultMapper.readValue(defaultSerialized, Språkkode.class);
        assertThat(defaultDeserialized).isEqualTo(original);
        final Språkkode openapiDeserialized = openapiMapper.readValue(openapiSerialized, Språkkode.class);
        assertThat(openapiDeserialized).isEqualTo(original);
    }

    @Test
    public void testBehandlingTypeDeSer() throws JsonProcessingException {
        final BehandlingType original = BehandlingType.REVURDERING;
        final String defaultSerialized = defaultMapper.writeValueAsString(original);
        assertThat(defaultSerialized).isEqualToIgnoringWhitespace("{\"kode\":\"BT-004\",\"kodeverk\":\"BEHANDLING_TYPE\"}");
        final String openapiSerialized = openapiMapper.writeValueAsString(original);
        assertThat(openapiSerialized).isEqualToIgnoringWhitespace("\"BT-004\"");
        final BehandlingType defaultDeserialized = defaultMapper.readValue(defaultSerialized, BehandlingType.class);
        assertThat(defaultDeserialized).isEqualTo(original);
        final BehandlingType openapiDeserialized = openapiMapper.readValue(openapiSerialized, BehandlingType.class);
        assertThat(openapiDeserialized).isEqualTo(original);
    }
}
