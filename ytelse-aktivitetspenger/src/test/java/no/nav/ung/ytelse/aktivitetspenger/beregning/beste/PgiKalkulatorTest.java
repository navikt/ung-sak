package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Month;
import java.time.Year;
import java.util.List;

import static no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BeregningTjeneste.lagBeregningInput;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PgiKalkulatorTest {

    @Test
    void avgrensÅrsinntekter_inntekt_mellom_6G_12G_avkortes_mot_6Gsnitt_2024() {
        var år2024 = Year.of(2024);
        var årsperiode = årsperiodeAv(år2024);
        var niG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(9));

        var resultat = new PgiKalkulator(lagBeregningInput(
            år2024,
            år2024.atDay(1),
            List.of(lagInntektspost(niG, årsperiode))
        )).avgrensÅrsinntekterUtenOppjustering();

        assertThat(resultat.get(år2024)).isEqualByComparingTo(new BigDecimal(733_350));
    }

    @Test
    void skal_beregne_med_oppjusteringsfaktor_for_tidligere_år_før_skatteoppgjør() {
        var sisteLigningsår = Year.of(2024);
        var skjæringstidspunkt = Year.of(2026).atDay(1);

        var inntektspost = lagInntektspost(BigDecimal.valueOf(500_000), årsperiodeAv(sisteLigningsår));

        var resultat = new PgiKalkulator(lagBeregningInput(
            sisteLigningsår,
            skjæringstidspunkt,
            List.of(inntektspost)
        )).avgrensOgOppjusterÅrsinntekter();

        // 130 160 kroner (Grunnbeløp start 2026) / 122 225 kroner (G-snitt 2024) * 500 000 = 532 460
        assertThat(resultat.get(sisteLigningsår)).isEqualByComparingTo(new BigDecimal("532460.6259000000"));
    }

    @Test
    void skal_beregne_med_oppjusteringsfaktor_for_tidligere_år_etter_skatteoppgjør() {
        var sisteLigningsår = Year.of(2025);
        var skjæringstidspunkt = Year.of(2026).atMonth(Month.APRIL).atEndOfMonth();

        var inntektspost = lagInntektspost(BigDecimal.valueOf(500_000), årsperiodeAv(sisteLigningsår));

        var resultat = new PgiKalkulator(lagBeregningInput(
            sisteLigningsår,
            skjæringstidspunkt,
            List.of(inntektspost)
        )).avgrensOgOppjusterÅrsinntekter();

        // 130 160 kroner (Grunnbeløp start 2026) / 128 116 kroner (G-snitt 2025) * 500 000 = 507 977
        assertThat(resultat.get(sisteLigningsår)).isEqualByComparingTo(new BigDecimal("507977.1457000000"));
    }

    @Test
    void skal_beregne_med_oppjusteringsfaktor_før_etter_g_justering() {
        var sisteLigningsår = Year.of(2024);
        var skjæringstidspunkt_før_justering = Year.of(2025).atMonth(Month.APRIL).atEndOfMonth();
        var skjæringstidspunkt_etter_justering = Year.of(2025).atMonth(Month.MAY).atEndOfMonth();

        var inntektspost = lagInntektspost(BigDecimal.valueOf(500_000), årsperiodeAv(sisteLigningsår));

        {
            var resultat = new PgiKalkulator(lagBeregningInput(
                sisteLigningsår,
                skjæringstidspunkt_før_justering,
                List.of(inntektspost)
            )).avgrensOgOppjusterÅrsinntekter();

            // 124 028 kroner (Grunnbeløp start 2025) / 122 225 kroner (G-snitt 2024) * 500 000 = 507 375
            assertThat(resultat.get(sisteLigningsår)).isEqualByComparingTo(new BigDecimal("507375.7414500000"));
        }

        {
            var resultat = new PgiKalkulator(lagBeregningInput(
                sisteLigningsår,
                skjæringstidspunkt_etter_justering,
                List.of(inntektspost)
            )).avgrensOgOppjusterÅrsinntekter();

            // 130 160 kroner (Grunnbeløp slutt 2025) / 122 225 kroner (G-snitt 2024) * 500 000 = 532 460
            assertThat(resultat.get(sisteLigningsår)).isEqualByComparingTo(new BigDecimal("532460.6259000000"));
        }
    }

    @Test
    void skal_beregne_korrekt_PGI_bidrag_nøyaktig_på_6G() {
        var år2024 = Year.of(2024);
        var periode = årsperiodeAv(år2024);

        // 6G snitt 2024 = 6 * 122 225 = 733 350
        var seksG = BigDecimal.valueOf(122_225).multiply(BigDecimal.valueOf(6));
        var inntektspost = lagInntektspost(seksG, periode);

        var resultat = new PgiKalkulator(lagBeregningInput(
            år2024,
            år2024.atDay(1),
            List.of(inntektspost)
        )).avgrensÅrsinntekterUtenOppjustering();

        assertThat(resultat.get(Year.of(2024))).isEqualByComparingTo(new BigDecimal(733_350));
    }

    private static Periode årsperiodeAv(Year år) {
        return new Periode(år.atDay(1), år.atMonth(Month.DECEMBER).atEndOfMonth());
    }

    private static Inntektspost lagInntektspost(BigDecimal verdi, Periode periode) {
        return InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(verdi)
            .medPeriode(periode.getFom(), periode.getTom())
            .build();
    }
}
