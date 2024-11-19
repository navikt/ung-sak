package no.nav.ung.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import no.nav.ung.kodeverk.api.Kodeverdi;
import org.junit.jupiter.api.Test;

class OpenapiEnumSerializerTest {
    private final ObjectMapper om;

    public OpenapiEnumSerializerTest() {
        final var moduleToTest = new SimpleModule();
        ObjectMapper baseObjectMapper = new ObjectMapper();
        moduleToTest.addSerializer(new OpenapiEnumSerializer(baseObjectMapper));
        moduleToTest.setDeserializerModifier(new OpenapiEnumBeanDeserializerModifier());
        this.om = baseObjectMapper.copy().registerModule(moduleToTest);
    }

    // Serialiserast som vanleg til enum name()
    private static enum TestEnum1 implements Kodeverdi {
        VERDI1,
        VERDI2;

        @Override
        public String getKode() {
            return this.name();
        }
        @Override
        public String getOffisiellKode() {
            return this.getKode();
        }

        @Override
        public String getKodeverk() {
            return "N/A";
        }

        @Override
        public String getNavn() {
            return "N/A";
        }
    }


    // Serialisering av denne blir overstyrt til toString() verdi, altså name()
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    private static enum TestEnum2 implements Kodeverdi {
        TEST_ENUM_2_V1("test_enum_2_kode1"),
        TEST_ENUM_2_V2("test_enum_2_kode2");

        private final String kode;

        private TestEnum2(final String kode) {
            this.kode = kode;
        }

        public String getKode() {
            return this.kode;
        }
        @Override
        public String getOffisiellKode() {
            return this.getKode();
        }

        @Override
        public String getKodeverk() {
            return "N/A";
        }

        @Override
        public String getNavn() {
            return "N/A";
        }
    }

    // Serialiserast som vanleg til @JsonValue verdi - getKode()
    private static enum TestEnum3 {
        TEST_ENUM_3_V1("test_enum_3_kode1"),
        TEST_ENUM_3_V2("test_enum_3_kode2");

        private final String kode;

        private TestEnum3(final String kode) {
            this.kode = kode;
        }

        @JsonValue
        public String getKode() {
            return this.kode;
        }
    }

    // Serialiserast som vanleg til @JsonValue verdi - getKode(), @JsonFormat = OBJECT blir automatisk ignorert.
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    private static enum TestEnum4 {
        TEST_ENUM_4_V1("test_enum_4_kode1"),
        TEST_ENUM_4_V2("test_enum_4_kode2");

        private final String kode;

        private TestEnum4(final String kode) {
            this.kode = kode;
        }

        @JsonValue
        public String getKode() {
            return this.kode;
        }

        // Simuler den vanlege no situasjonen i Kodeverdi enums, at det finnast JsonCreator som ikkje fungerer med name
        // input. Dette vil dermed feile i standard deserialisering når serialisering har skjedd med OpenapiEnumSerializer.
        // Med ny OpenapiEnumBeanDeserializerModifier skal det fungere.
        @JsonCreator
        public static TestEnum4 fraObjekt(@JsonProperty("kode") String kode) {
            for(var v : values()) {
                if(v.getKode().equals(kode)) {
                    return v;
                }
            }
            throw new IllegalArgumentException("TestEnum4 med kode " + kode + " ikke funnet");
        }
    }
    // Serialisering av denne blir overstyrt til å konvertere @JsonValue verdi return med toString().
    // Openapi enums må vere strings.
    private static enum TestEnum5 {
        TEST_ENUM_5_V1(1001),
        TEST_ENUM_5_V2(1002);

        private final int number;

        private TestEnum5(final int number) {
            this.number = number;
        }

        @JsonValue
        public int getNumber() {
            return this.number;
        }
    }

    // Denne skal feile sidan verdi blir boolean.
    private static enum InkompatibelEnum6 {
        TEST_ENUM_6_V1(true),
        TEST_ENUM_6_V2(false);

        private final boolean b;

        private InkompatibelEnum6(final boolean b) {
            this.b = b;
        }

        @JsonValue
        public boolean getB() {
            return this.b;
        }
    }

    private static class EnumsContainer {
        public TestEnum1 enum1 = TestEnum1.VERDI1;
        public TestEnum2 enum2 = TestEnum2.TEST_ENUM_2_V1;
        public TestEnum3 enum3 = TestEnum3.TEST_ENUM_3_V1;
        public TestEnum4 enum4 = TestEnum4.TEST_ENUM_4_V1;
        public TestEnum5 enum5 = TestEnum5.TEST_ENUM_5_V2;

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof EnumsContainer that)) return false;
            return enum1 == that.enum1 &&
                enum2 == that.enum2 &&
                enum3 == that.enum3 &&
                enum4 == that.enum4 &&
                enum5 == that.enum5;
        }
    }

    @Test
    public void diverse_enums_skal_serialisere_og_deserialisere_openapi_kompatibelt() throws JsonProcessingException {
        final var input = new EnumsContainer();
        final String json = this.om.writeValueAsString(input);
        final String expected = "{\"enum1\":\"VERDI1\",\"enum2\":\"TEST_ENUM_2_V1\",\"enum3\":\"test_enum_3_kode1\",\"enum4\":\"test_enum_4_kode1\",\"enum5\":\"1002\"}";
        assertThat(json).isEqualTo(expected);
        final EnumsContainer deserialized = this.om.readValue(json, EnumsContainer.class);
        assertThat(deserialized).isEqualTo(input);
    }

    @Test
    public void inkompatible_enums_skal_feile() {
        final var input = InkompatibelEnum6.TEST_ENUM_6_V1;
        assertThatExceptionOfType(JsonMappingException.class).isThrownBy(() -> {
            this.om.writeValueAsString(input);
        }).withMessageStartingWith("enum ville blitt serialisert til");
    }
}
