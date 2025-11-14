package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.ytelse.BeregnetSats;
import no.nav.ung.sak.ytelse.TilkjentYtelseBeregner;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TilkjentYtelseBeregnerTest {

    @Test
    void test_null_verdier() {
        // Test for null values
        assertThrows(NullPointerException.class, () -> {
            beregn(null, null, null);
        });
    }

    @Test
    void test_fom_og_tom_på_samme_dag() {
        LocalDateInterval di = new LocalDateInterval(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 10));
        BeregnetSats sats = new BeregnetSats(BigDecimal.valueOf(1000), 0);
        int rapporertinntekt = 500;

        final var resultat = beregn(di, sats, BigDecimal.valueOf(rapporertinntekt));

        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(BigDecimal.valueOf(983.5));
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.valueOf(984));
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(98.4));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.valueOf(16.5));
    }

    @Test
    void test_periode_lik_hele_måneden() {
        LocalDateInterval di = new LocalDateInterval(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28));
        BeregnetSats sats = new BeregnetSats(BigDecimal.valueOf(1000), 0);
        int rapporertinntekt = 500;

        final var resultat = beregn(di, sats, BigDecimal.valueOf(rapporertinntekt));

        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(BigDecimal.valueOf(670));
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.valueOf(34));
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(68));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.valueOf(330));
    }

    private static TilkjentYtelseVerdi beregn(LocalDateInterval di, BeregnetSats sats, BigDecimal rapporertinntekt) {
        return TilkjentYtelseBeregner.beregn(di, sats, rapporertinntekt).verdi();
    }

    @Test
    void test_redusert_beløp_lik_grunnsats() {
        // Test for boundary values for payment degree
        final var grunnsatsBeløp = BigDecimal.valueOf(1000);
        BeregnetSats sats = new BeregnetSats(grunnsatsBeløp, 0);
        int redusertBeløpLikGrunnsats = 1000;

        final var resultat = beregn(new LocalDateInterval(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 10).plusDays(1)), sats, BigDecimal.valueOf(redusertBeløpLikGrunnsats));

        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(BigDecimal.valueOf(934));
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.valueOf(467));
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(93.4));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.valueOf(66));
    }

    @Test
    void test_redusert_beløp_lik_null() {
        final var grunnsatsBeløp = BigDecimal.valueOf(1000);
        BeregnetSats sats = new BeregnetSats(grunnsatsBeløp, 0);
        int redusertBeløpNull = 0;

        final var resultat = beregn(new LocalDateInterval(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 10).plusDays(1)), sats, BigDecimal.valueOf(redusertBeløpNull));

        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(grunnsatsBeløp);
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void test_hel_måned() {
        LocalDateInterval helMåned = new LocalDateInterval(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 10, 31)); // Full month

        BeregnetSats sats = new BeregnetSats(BigDecimal.valueOf(1000), 0);
        int rapporertinntekt = 500;

        final var resultat = beregn(helMåned, sats, BigDecimal.valueOf(rapporertinntekt));

        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(BigDecimal.valueOf(670));
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(66.6666666667));
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.valueOf(330));
    }

    @Test
    void test_kun_helg() {
        LocalDateInterval helg = new LocalDateInterval(LocalDate.of(2025, 2, 8), LocalDate.of(2025, 2, 9)); // Weekend

        final var grunnsatsBeløp = BigDecimal.valueOf(1000);
        BeregnetSats sats = new BeregnetSats(grunnsatsBeløp, 0);
        int rapporertinntekt = 500;

        final var resultat = beregn(helg, sats, BigDecimal.valueOf(rapporertinntekt));


        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void test_reduksjon_mindre_enn_barnetillegg() {
        LocalDateInterval di = new LocalDateInterval(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 10).plusDays(1));
        BigDecimal grunnsatsBeløp = BigDecimal.valueOf(1000);
        BigDecimal barnetillegg = BigDecimal.valueOf(200);
        BeregnetSats sats = new BeregnetSats(grunnsatsBeløp, barnetillegg.intValue());
        int rapporertinntekt = 100;

        final var resultat = beregn(di, sats, BigDecimal.valueOf(rapporertinntekt));

        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(BigDecimal.valueOf(1193.4));
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.valueOf(597));
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(99.5));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.valueOf(6.6));
    }

    @Test
    void test_reduksjon_lik_barnetillegg() {
        LocalDateInterval di = new LocalDateInterval(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 10).plusDays(1));
        BigDecimal grunnsatsBeløp = BigDecimal.valueOf(1000);
        BigDecimal barnetillegg = BigDecimal.valueOf(132);
        BeregnetSats sats = new BeregnetSats(grunnsatsBeløp, barnetillegg.intValue());
        int rapporertinntekt = 200;

        final var resultat = beregn(di, sats, BigDecimal.valueOf(rapporertinntekt));

        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(BigDecimal.valueOf(1118.8));
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.valueOf(559));
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(98.7632508834));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.valueOf(13.2));
    }

    @Test
    void test_reduksjon_større_enn_barnetillegg() {
        LocalDateInterval di = new LocalDateInterval(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 10).plusDays(1));
        BigDecimal grunnsatsBeløp = BigDecimal.valueOf(1000);
        BigDecimal barnetillegg = BigDecimal.valueOf(200);
        BeregnetSats sats = new BeregnetSats(grunnsatsBeløp, barnetillegg.intValue());
        int rapporertinntekt = 500;

        final var resultat = beregn(di, sats, BigDecimal.valueOf(rapporertinntekt));

        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(BigDecimal.valueOf(1167));
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.valueOf(584));
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(97.3333333333));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.valueOf(33));
    }

    @Test
    void test_reduksjon_som_ikke_utgjør_endring_i_dagsats() {
        LocalDateInterval di = new LocalDateInterval(LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 10).plusDays(10));
        BigDecimal grunnsatsBeløp = BigDecimal.valueOf(1000);
        BigDecimal barnetillegg = BigDecimal.valueOf(132);
        BeregnetSats sats = new BeregnetSats(grunnsatsBeløp, barnetillegg.intValue());
        int rapporertinntekt = 1;

        final var resultat = beregn(di, sats, BigDecimal.valueOf(rapporertinntekt));

        assertThat(resultat.redusertBeløp()).isEqualByComparingTo(BigDecimal.valueOf(1131.703));
        assertThat(resultat.dagsats()).isEqualByComparingTo(BigDecimal.valueOf(126));
        assertThat(resultat.utbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.reduksjon()).isEqualByComparingTo(BigDecimal.valueOf(0.297));
    }




}
