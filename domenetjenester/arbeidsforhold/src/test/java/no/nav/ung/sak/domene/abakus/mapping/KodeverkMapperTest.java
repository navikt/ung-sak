package no.nav.ung.sak.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.kodeverk.InntektskildeType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class KodeverkMapperTest {

    @ParameterizedTest
    @EnumSource(value = InntektskildeType.class, names = {"INNTEKT_UNGDOMSYTELSE", "INNTEKT_AKTIVITETSPENGER", "SIGRUN"})
    void skal_mappe_inntektskilder_fra_abakus_og_tilbake(InntektskildeType dto) {
        InntektsKilde inntektsKilde = KodeverkMapper.mapInntektsKildeFraDto(dto);

        assertThat(inntektsKilde.getKode()).isEqualTo(dto.getKode());
        assertThat(KodeverkMapper.mapInntektsKildeTilDto(inntektsKilde)).isEqualTo(dto);
    }
}
