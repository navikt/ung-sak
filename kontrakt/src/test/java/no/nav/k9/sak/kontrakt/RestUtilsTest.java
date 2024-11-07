package no.nav.k9.sak.kontrakt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;

public class RestUtilsTest {

    public static final ObjectMapper OM = RestUtils.getObjectMapper();

    @Test
    public void test_deserialiser_BehandlingId_as_String() throws Exception {
        var id = new BehandlingIdDto(10L);
        var json = OM.writeValueAsString(id);

        assertThat(json).isNotNull();
        var dto = OM.readValue(json, BehandlingIdDto.class);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(id.getId());
    }

    @Test
    public void test_deserialiser_BehandlingId_as_uuid() throws Exception {
        var id = new BehandlingIdDto(UUID.nameUUIDFromBytes("hello".getBytes()));
        var json = OM.writeValueAsString(id);

        assertThat(json).isNotNull();
        var dto = OM.readValue(json, BehandlingIdDto.class);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(id.getId());
    }

    @Test
    public void test_deserialiser_BehandlingId_as_Long() throws Exception {
        String json = "{\"behandlingId\":99}";
        var dto = OM.readValue(json, BehandlingIdDto.class);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("99");
    }

}
