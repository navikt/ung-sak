package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BesteBeregningTest {

    private static final LocalDate VIRKNINGSDATO = LocalDate.of(2026, 3, 1);

    @Test
    void siste_år_brukes_når_det_er_høyere_enn_snitt_av_tre_år() {
        var inntektsposter = List.of(
            lagInntektspost(2025, BigDecimal.valueOf(600_000)),
            lagInntektspost(2024, BigDecimal.valueOf(200_000)),
            lagInntektspost(2023, BigDecimal.valueOf(200_000))
        );

        var resultat = new BesteBeregning(VIRKNINGSDATO).avgjørBestePGI(inntektsposter);
        assertThat(resultat.getÅrsinntektBesteBeregning()).isEqualByComparingTo(resultat.getÅrsinntektSisteÅr());
    }

    @Test
    void snitt_av_tre_år_brukes_når_det_er_høyere_enn_siste_år() {
        var inntektsposter = List.of(
            lagInntektspost(2025, BigDecimal.valueOf(100_000)),
            lagInntektspost(2024, BigDecimal.valueOf(500_000)),
            lagInntektspost(2023, BigDecimal.valueOf(500_000))
        );

        var resultat = new BesteBeregning(VIRKNINGSDATO).avgjørBestePGI(inntektsposter);
        assertThat(resultat.getÅrsinntektBesteBeregning()).isEqualByComparingTo(resultat.getÅrsinntektSisteTreÅr());
    }

    @Test
    void mangler_inntektsposter_tre_år_tilbake_skal_velge_siste_året_selv_om_snitt_av_to_år_er_høyere() {
        var inntektsposter = List.of(
            lagInntektspost(2025, BigDecimal.valueOf(300_000)),
            lagInntektspost(2024, BigDecimal.valueOf(400_000))
            // 2023 mangler
        );

        var resultat = new BesteBeregning(VIRKNINGSDATO).avgjørBestePGI(inntektsposter);
        assertThat(resultat.getÅrsinntektBesteBeregning()).isEqualByComparingTo(resultat.getÅrsinntektSisteÅr());
    }

    @Test
    void ingen_lignet_inntekt_siste_2_år_skal_bruke_snitt_av_tre_år() {
        var inntektsposter = List.of(
            // 2025 og 2024 mangler
            lagInntektspost(2023, BigDecimal.valueOf(300_000))
        );

        var resultat = new BesteBeregning(VIRKNINGSDATO).avgjørBestePGI(inntektsposter);

        assertThat(resultat.getÅrsinntektSisteÅr()).isEqualByComparingTo(BigDecimal.ZERO);
        // 130 160 (G-snitt 2026) / 116 239 (G-snitt 2023) * 300 000/ 3 = 111 976,18699
        assertThat(resultat.getÅrsinntektBesteBeregning()).isEqualByComparingTo(new BigDecimal("111976.18699"));
    }

    @Test
    void ingen_inntektsposter_gir_null_resultat() {
        var resultat = new BesteBeregning(VIRKNINGSDATO).avgjørBestePGI(List.of());
        assertThat(resultat.getÅrsinntektBesteBeregning()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static Inntektspost lagInntektspost(int år, BigDecimal beløp) {
        return InntektspostBuilder.ny()
            .medPeriode(LocalDate.of(år, 1, 1), LocalDate.of(år, 12, 31))
            .medBeløp(beløp)
            .build();
    }
}
