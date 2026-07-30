package no.nav.ung.sak.domene.typer.tid;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

class JsonObjectMapperKodeverdiSomStringSerializerTest {

    @Test
    void skal_serializere_kodeverdi_til_string() throws IOException {
        String json = JsonObjectMapperKodeverdiSomStringSerializer.getJson(new TestSerializerDto(FagsakYtelseType.PLEIEPENGER_SYKT_BARN));
        assertThat(json).isEqualToIgnoringWhitespace("{\"sakstype\": \"PSB\"}");
    }

    @Test
    void skal_deserialisere_kodestring() throws IOException {
        final TestSerializerDto input = new TestSerializerDto(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        String json = JsonObjectMapperKodeverdiSomStringSerializer.getJson(input);
        assertThat(json).isEqualToIgnoringWhitespace("{\"sakstype\": \"PSB\"}");
        TestSerializerDto deserialized = JsonObjectMapper.fromJson(json, TestSerializerDto.class);
        assertThat(deserialized).isEqualTo(input);
    }

    @JsonInclude(value = JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    private static class TestSerializerDto {
        @JsonProperty(value = "sakstype")
        public FagsakYtelseType fagsakYtelseType;

        public TestSerializerDto(FagsakYtelseType fagsakYtelseType) {
            this.fagsakYtelseType = fagsakYtelseType;
        }

        public TestSerializerDto() {
            this(null);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof TestSerializerDto that)) return false;
            return fagsakYtelseType == that.fagsakYtelseType;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(fagsakYtelseType);
        }
    }
}
