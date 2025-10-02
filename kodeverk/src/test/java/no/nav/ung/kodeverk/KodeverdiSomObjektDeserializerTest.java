package no.nav.ung.kodeverk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KodeverdiSomObjektDeserializerTest {
    private ObjectMapper om = new ObjectMapper();

    private record TstDto(
        KodeverdiSomObjekt<DokumentMalType> dokumentMalType,
        KodeverdiSomObjekt<BehandlingStatus> behandlingStatus,
        Long someNum
    ) {
        public TstDto(DokumentMalType dokumentMalType, BehandlingStatus behandlingStatus, Long someNum) {
            this(new KodeverdiSomObjekt<>(dokumentMalType), new KodeverdiSomObjekt<>(behandlingStatus), someNum);
        }
    }

    private record TstKodeverdi(String kode) implements Kodeverdi {
        @Override
        public String getKode() {
            return this.kode;
        }

        @Override
        public String getKodeverk() {
            return this.kode;
        }

        @Override
        public String getNavn() {
            return this.kode;
        }
    }

    @Test
    void test_deserializer_direct() throws JsonProcessingException {
        final KodeverdiSomObjekt<DokumentMalType> original = new KodeverdiSomObjekt<>(DokumentMalType.ENDRING_HØY_SATS);
        final var json = this.om.writeValueAsString(original);
        final KodeverdiSomObjekt<DokumentMalType> deserialized = this.om.readValue(json, new TypeReference<KodeverdiSomObjekt<DokumentMalType>>() {});
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void test_deserializer_indirect() throws JsonProcessingException {
        final var original = new TstDto(DokumentMalType.ENDRING_HØY_SATS, BehandlingStatus.AVSLUTTET, 123L);
        final var json = this.om.writeValueAsString(original);
        final var deserialized = this.om.readValue(json, TstDto.class);
        assertThat(deserialized).isEqualTo(original);
    }


    @Test
    void test_fail() throws JsonProcessingException {
        final KodeverdiSomObjekt<TstKodeverdi> tst = new KodeverdiSomObjekt<>(new TstKodeverdi("wrong"));
        final var json = this.om.writeValueAsString(tst);
        final var thrown = assertThrows(IllegalStateException.class, () -> {
            this.om.readValue(json, new TypeReference<KodeverdiSomObjekt<TstKodeverdi>>() {});
        });
        assertThat(thrown.getMessage()).contains("Could not deserialize KodeverdiSomObjekt with kode: \"wrong\". Generic argument Lno/nav/ung/kodeverk/KodeverdiSomObjektDeserializerTest$TstKodeverdi; is not an enum");
    }
}
