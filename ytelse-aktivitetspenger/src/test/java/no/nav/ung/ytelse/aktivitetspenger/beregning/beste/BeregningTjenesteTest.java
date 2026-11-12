package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BeregningTjeneste.lagBeregningInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BeregningTjenesteTest {

    private static final LocalDate VIRKNINGSDATO = LocalDate.of(2026, 5, 2);
    private static final Year SIST_LIGNEDE_ÅR = Year.of(2025);

    @Test
    void siste_år_brukes_når_det_er_høyere_enn_snitt_av_tre_år() {
        var inntektsposter = List.of(
            lagInntektspost(2025, BigDecimal.valueOf(600_000)),
            lagInntektspost(2024, BigDecimal.valueOf(200_000)),
            lagInntektspost(2023, BigDecimal.valueOf(200_000))
        );

        var resultat = BeregningTjeneste.avgjørBesteberegning(lagBeregningInput(SIST_LIGNEDE_ÅR, VIRKNINGSDATO, inntektsposter));
        assertThat(resultat.getBeregningsgrunnlag()).isEqualByComparingTo(resultat.getÅrsinntektSisteÅr());
    }

    @Test
    void snitt_av_tre_år_brukes_når_det_er_høyere_enn_siste_år() {
        var inntektsposter = List.of(
            lagInntektspost(2025, BigDecimal.valueOf(100_000)),
            lagInntektspost(2024, BigDecimal.valueOf(500_000)),
            lagInntektspost(2023, BigDecimal.valueOf(500_000))
        );

        var resultat = BeregningTjeneste.avgjørBesteberegning(lagBeregningInput(SIST_LIGNEDE_ÅR, VIRKNINGSDATO, inntektsposter));
        assertThat(resultat.getBeregningsgrunnlag()).isEqualByComparingTo(resultat.getÅrsinntektSisteTreÅr());
    }

    @Test
    void mangler_inntektsposter_tre_år_tilbake_skal_velge_siste_året_selv_om_snitt_av_to_år_er_høyere() {
        var inntektsposter = List.of(
            lagInntektspost(2025, BigDecimal.valueOf(300_000)),
            lagInntektspost(2024, BigDecimal.valueOf(400_000))
            // 2023 mangler
        );

        var resultat = BeregningTjeneste.avgjørBesteberegning(lagBeregningInput(SIST_LIGNEDE_ÅR, VIRKNINGSDATO, inntektsposter));
        assertThat(resultat.getBeregningsgrunnlag()).isEqualByComparingTo(resultat.getÅrsinntektSisteÅr());
    }

    @Test
    void ingen_lignet_inntekt_siste_2_år_skal_bruke_snitt_av_tre_år() {
        var inntektsposter = List.of(
            // 2025 og 2024 mangler
            lagInntektspost(2023, BigDecimal.valueOf(300_000))
        );

        var resultat = BeregningTjeneste.avgjørBesteberegning(lagBeregningInput(SIST_LIGNEDE_ÅR, VIRKNINGSDATO, inntektsposter));

        assertThat(resultat.getÅrsinntektSisteÅr()).isEqualByComparingTo(BigDecimal.ZERO);
        // 130 160 (G-snitt 2026) / 116 239 (G-snitt 2023) * 300 000/ 3 = 111 976,18699
        assertThat(resultat.getBeregningsgrunnlag()).isEqualByComparingTo(new BigDecimal("117472.62107"));
    }

    @Test
    void ingen_inntektsposter_gir_null_resultat() {
        var resultat = BeregningTjeneste.avgjørBesteberegning(lagBeregningInput(SIST_LIGNEDE_ÅR, VIRKNINGSDATO, List.of()));
        assertThat(resultat.getBeregningsgrunnlag()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void siste_lignede_år_er_året_før_stp_fra_dagen_ligningsverdi_kan_være_tilgjengelig() {
        var inntektsposter = List.of(
            lagInntektspost(2025, BigDecimal.ZERO),
            lagInntektspost(2024, BigDecimal.ZERO)
        );

        assertThat(utledSisteLignedeÅr(LocalDate.of(2026, 5, 1), inntektsposter)).isEqualTo(Year.of(2025));
    }

    @Test
    void siste_lignede_år_faller_tilbake_til_seneste_inntektspost_når_året_før_stp_ikke_er_lignet() {
        var inntektsposter = List.of(
            lagInntektspost(2024, BigDecimal.ZERO),
            lagInntektspost(2023, BigDecimal.ZERO)
        );

        assertThat(utledSisteLignedeÅr(LocalDate.of(2026, 5, 1), inntektsposter)).isEqualTo(Year.of(2024));
        assertThat(utledSisteLignedeÅr(LocalDate.of(2026, 11, 30), inntektsposter)).isEqualTo(Year.of(2024));
    }

    @Test
    void siste_lignede_år_faller_tilbake_til_seneste_inntektspost_også_etter_grensen_for_når_ligningsverdi_må_være_tilgjengelig() {
        var inntektsposter = List.of(
            lagInntektspost(2024, BigDecimal.ZERO),
            lagInntektspost(2023, BigDecimal.ZERO)
        );

        assertThat(utledSisteLignedeÅr(LocalDate.of(2026, 12, 1), inntektsposter)).isEqualTo(Year.of(2024));

        // Bruker siste tilgjengelig ligningsår fra skatt også etter fristen, men logger warning
        assertThat(utledSisteLignedeÅr(LocalDate.of(2026, 12, 31), inntektsposter)).isEqualTo(Year.of(2024));
    }

    @Test
    void siste_lignede_år_er_året_før_stp_selv_om_det_finnes_inntektspost_for_stp_året() {
        var inntektsposter = List.of(
            lagInntektspost(2026, BigDecimal.ZERO),
            lagInntektspost(2025, BigDecimal.ZERO),
            lagInntektspost(2024, BigDecimal.ZERO)
        );
        var skjæringstidspunkt = LocalDate.of(2026, 6, 1);
        assertThat(utledSisteLignedeÅr(skjæringstidspunkt, inntektsposter)).isEqualTo(Year.of(2025));
    }

    @Test
    void siste_lignede_år_kaster_exception_når_seneste_inntektspost_er_mer_enn_to_år_før_stp() {
        var inntektsposter = List.of(
            lagInntektspost(2023, BigDecimal.ZERO)
        );
        var skjæringstidspunkt = LocalDate.of(2026, 6, 1);

        assertThatThrownBy(() -> utledSisteLignedeÅr(skjæringstidspunkt, inntektsposter))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void siste_lignede_år_avhenger_av_stp_året_og_ikke_dagens_dato() {
        var inntektsposter = List.of(
            lagInntektspost(2024, BigDecimal.ZERO),
            lagInntektspost(2023, BigDecimal.ZERO)
        );

        assertThat(utledSisteLignedeÅr(LocalDate.of(2025, 4, 30), inntektsposter)).isEqualTo(Year.of(2023));
        assertThat(utledSisteLignedeÅr(LocalDate.of(2025, 5, 1), inntektsposter)).isEqualTo(Year.of(2024));
    }

    private static Year utledSisteLignedeÅr(LocalDate skjæringstidspunkt, List<Inntektspost> inntektsposter) {
        return new BeregningTjeneste().utledSisteLignedeÅr(skjæringstidspunkt, inntektsposter);
    }

    private static Inntektspost lagInntektspost(int år, BigDecimal beløp) {
        return InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medPeriode(LocalDate.of(år, 1, 1), LocalDate.of(år, 12, 31))
            .medBeløp(beløp)
            .build();
    }
}
