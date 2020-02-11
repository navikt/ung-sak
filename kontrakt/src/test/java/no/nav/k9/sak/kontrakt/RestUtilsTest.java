package no.nav.k9.sak.kontrakt;

import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;

public class RestUtilsTest {

    @Test
    public void test_deserialiser_BehandlingUuid() throws Exception {
        var uuid = new BehandlingUuidDto(UUID.nameUUIDFromBytes("hello".getBytes()));
        var result = RestUtils.convertObjectToQueryString(uuid);
        Assertions.assertThat(result).isNotNull();
    }
}
