package no.nav.k9.sak.domene.typer.tid;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

class JsonObjectMapperKodeverdiSerializerTest {

    @Test
    void skal_serializere_kodeverdi_til_string() throws IOException {
        String json = JsonObjectMapperKodeverdiSerializer.getJson(new TestSerializerDto(FagsakYtelseType.PLEIEPENGER_SYKT_BARN));
        assertThat(json).isEqualToIgnoringWhitespace("{\"sakstype\": \"PSB\"}");
    }

    @JsonInclude(value = JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    private static class TestSerializerDto {
        @JsonProperty(value = "sakstype")
        private FagsakYtelseType fagsakYtelseType;

        public TestSerializerDto(FagsakYtelseType fagsakYtelseType) {
            this.fagsakYtelseType = fagsakYtelseType;
        }
    }
}
