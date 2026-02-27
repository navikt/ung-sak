package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PgiUtregnerTest {

    private static final BigDecimal G = BigDecimal.valueOf(100000);

    @Test
    void inntekt_under_6G_returnerer_inntekten_skalert_med_oppjusteringsfaktor() {
        var inntekt = G.multiply(BigDecimal.valueOf(4));
        var oppjusteringsfaktor = BigDecimal.valueOf(1.05);

        var resultat = lagUtregner(inntekt, G, oppjusteringsfaktor).avgrensOgOppjusterårsinntekt();

        var forventet = inntekt.multiply(oppjusteringsfaktor);
        assertThat(resultat).isEqualByComparingTo(forventet);
    }

    @Test
    void inntekt_nøyaktig_6G_behandles_som_under_6G() {
        var inntekt = G.multiply(BigDecimal.valueOf(6));
        var oppjusteringsfaktor = BigDecimal.ONE;

        var resultat = lagUtregner(inntekt, G, oppjusteringsfaktor).avgrensOgOppjusterårsinntekt();
        assertThat(resultat).isEqualByComparingTo(inntekt);
    }

    @Test
    void inntekt_over_6G_er_begrenset_til_6G() {
        var inntekt = G.multiply(BigDecimal.valueOf(20));
        var oppjusteringsfaktor = BigDecimal.ONE;

        var resultat = lagUtregner(inntekt, G, oppjusteringsfaktor).avgrensOgOppjusterårsinntekt();

        assertThat(resultat).isEqualByComparingTo(G.multiply(BigDecimal.valueOf(6)));
    }


    private PgiKalkulator.PgiUtregner lagUtregner(BigDecimal inntekt, BigDecimal gSnitt, BigDecimal oppjusteringsfaktor) {
        return new PgiKalkulator.PgiUtregner(inntekt)
            .setGrunnbeløpSnitt(gSnitt)
            .setoppjusteringsfaktor(oppjusteringsfaktor);
    }
}
