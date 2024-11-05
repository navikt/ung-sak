package no.nav.k9.kodeverk.vilkårtype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VilkårTypeTest {
    final ObjectMapper om = new ObjectMapper();

    @Test
    public void testLegacySerialiseringDeserialisering() throws JsonProcessingException {
        final var original = VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
        final String serialisert = om.writeValueAsString(original);
        assertThat(serialisert).containsIgnoringWhitespaces("\"kode\": \""+ original.getKode() +"\"");
        final VilkårType deserialisert = om.readValue(serialisert, VilkårType.class);
        assertThat(deserialisert).isEqualTo(original);
    }
}
