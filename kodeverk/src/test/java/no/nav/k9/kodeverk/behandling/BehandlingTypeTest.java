package no.nav.k9.kodeverk.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BehandlingTypeTest {

    private ObjectMapper om = new ObjectMapper();

    @Test
    public void sjekk_rendering() throws Exception {
        String json1 = om.writeValueAsString(BehandlingType.REVURDERING);
        System.out.println(json1);

        var t = om.reader().forType(BehandlingType.class).readValue(json1);
        assertThat(t).isNotNull();

        // forsøker med ren key input også
        var t2 = om.reader().forType(BehandlingType.class).readValue("\"" + BehandlingType.REVURDERING.getKode() + "\"");
        assertThat(t2).isNotNull().isSameAs(t);

        var t3 = om.reader().forType(BehandlingType.class).readValue("{\"kode\":\"BT-004\",\"kodeverk\":\"BEHANDLING_TYPE\"}");
        assertThat(t3).isNotNull().isSameAs(t3);

    }
}
