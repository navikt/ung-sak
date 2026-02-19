package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.sak.typer.Beløp;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester for PgiUtregner:
 *   <= 6G:           pgi * (grunnbeløp / gSnitt)
 *   6G < x <= 12G:  (6G + (pgi - 6G) / 3) * (grunnbeløp / gSnitt)
 *   > 12G:           8G * (grunnbeløp / gSnitt)
 */
class PgiUtregnerTest {

    private static final BigDecimal G = BigDecimal.valueOf(100000);

    @Test
    void inntekt_under_6G_returnerer_inntekten_skalert_med_inflasjonsfaktor() {
        var inntekt = G.multiply(BigDecimal.valueOf(4));
        var inflasjonsfaktor = BigDecimal.valueOf(1.05);

        var resultat = lagUtregner(inntekt, G, inflasjonsfaktor).beregnPGI();

        var forventet = inntekt.multiply(inflasjonsfaktor);
        assertThat(resultat).isEqualByComparingTo(forventet);
    }

    @Test
    void inntekt_noeyaktig_6G_behandles_som_under_6G() {
        var inntekt = G.multiply(BigDecimal.valueOf(6));
        var inflasjonsfaktor = BigDecimal.ONE;

        var resultat = lagUtregner(inntekt, G, inflasjonsfaktor).beregnPGI();
        assertThat(resultat).isEqualByComparingTo(inntekt);
    }

    @Test
    void inntekt_mellom_6G_og_12G_gir_redusert_pgi() {
        var inntekt9G = G.multiply(BigDecimal.valueOf(9));
        var inflasjonsfaktor = BigDecimal.ONE;

        var resultat = lagUtregner(inntekt9G, G, inflasjonsfaktor).beregnPGI();

        // 6G + (9G - 6G) / 3 = 6G + G = 7G
        assertThat(resultat).isEqualByComparingTo(BigDecimal.valueOf(700_000));
    }

    @Test
    void inntekt_noeyaktig_12G_gir_8G() {
        var inntekt = G.multiply(BigDecimal.valueOf(12));
        var inflasjonsfaktor = BigDecimal.ONE;

        var resultat = lagUtregner(inntekt, G, inflasjonsfaktor).beregnPGI();

        // 6G + (12G - 6G) / 3 = 6G + 2G = 8G
        assertThat(resultat).isEqualByComparingTo(BigDecimal.valueOf(800_000));
    }

    @Test
    void inntekt_over_12G_er_begrenset_til_8G() {
        var inntekt = G.multiply(BigDecimal.valueOf(20));
        var inflasjonsfaktor = BigDecimal.ONE;

        var resultat = lagUtregner(inntekt, G, inflasjonsfaktor).beregnPGI();

        assertThat(resultat).isEqualByComparingTo(BigDecimal.valueOf(800_000));
    }


    private static FinnGjennomsnittligPGI.PgiUtregner lagUtregner(BigDecimal inntekt, BigDecimal gSnitt, BigDecimal inflasjonsfaktor) {
        return new FinnGjennomsnittligPGI.PgiUtregner(inntekt)
            .setGrunnbeløpSnitt(gSnitt)
            .setInflasjonsfaktor(inflasjonsfaktor);
    }
}
