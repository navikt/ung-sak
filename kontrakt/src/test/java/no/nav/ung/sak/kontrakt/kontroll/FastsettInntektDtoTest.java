package no.nav.ung.sak.kontrakt.kontroll;

import no.nav.ung.kontrakt.JsonUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class FastsettInntektDtoTest {

    @Test
    void skal_serialisere_riktig() throws IOException {


        final var fastsettInntektDto = JsonUtil.fromJson("""
            {
                  "@type": "8000",
                  "kode": "8000",
                  "begrunnelse": "test",
                  "perioder": [
                    {
                      "periode": { "fom": "2025-03-01", "tom": "2025-03-31" },
                      "valg": "BRUK_REGISTER_INNTEKT",
                      "begrunnelse": "test1"
                    },
                    {
                      "periode": { "fom": "2025-02-01", "tom": "2025-02-28" },
                      "fastsattInnntekt": 213,
                      "valg": "MANUELT_FASTSATT",
                      "begrunnelse": "test2"
                    }
                  ]
                }
            """, FastsettInntektDto.class);


        assertThat(fastsettInntektDto.getKode()).isEqualTo("8000");
        assertThat(fastsettInntektDto.getBegrunnelse()).isEqualTo("test");
        assertThat(fastsettInntektDto.getPerioder().size()).isEqualTo(2);

        assertThat(fastsettInntektDto.getPerioder().get(0).fastsattInnntekt()).isNull();
        assertThat(fastsettInntektDto.getPerioder().get(0).periode().getFom()).isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(fastsettInntektDto.getPerioder().get(0).periode().getTom()).isEqualTo(LocalDate.of(2025, 3, 31));
        assertThat(fastsettInntektDto.getPerioder().get(0).valg()).isEqualTo(BrukKontrollertInntektValg.BRUK_REGISTER_INNTEKT);
        assertThat(fastsettInntektDto.getPerioder().get(0).begrunnelse()).isEqualTo("test1");

        assertThat(fastsettInntektDto.getPerioder().get(1).fastsattInnntekt()).isEqualTo(213);
        assertThat(fastsettInntektDto.getPerioder().get(1).periode().getFom()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(fastsettInntektDto.getPerioder().get(1).periode().getTom()).isEqualTo(LocalDate.of(2025, 2, 28));
        assertThat(fastsettInntektDto.getPerioder().get(1).valg()).isEqualTo(BrukKontrollertInntektValg.MANUELT_FASTSATT);
        assertThat(fastsettInntektDto.getPerioder().get(1).begrunnelse()).isEqualTo("test2");
    }
}
