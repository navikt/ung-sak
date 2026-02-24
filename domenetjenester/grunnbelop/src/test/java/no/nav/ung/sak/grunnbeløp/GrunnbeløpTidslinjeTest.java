package no.nav.ung.sak.grunnbeløp;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GrunnbeløpTidslinjeTest {

    @Test
    void skal_returnere_vektet_gjennomsnittlig_grunnbeløp_per_år() {
        LocalDateTimeline<Grunnbeløp> snittTidslinje = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();

        for (LocalDateSegment<Grunnbeløp> segment : snittTidslinje.toSegments()) {
            int år = Year.of(segment.getFom().getYear()).getValue();

            assertEquals(LocalDate.of(år, 1, 1), segment.getFom());
            assertEquals(LocalDate.of(år, 12, 31), segment.getTom());
        }
    }

    @Test
    void skal_beregne_vektet_snitt_for_2024() {
        LocalDateTimeline<Grunnbeløp> snittTidslinje = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();

        LocalDateSegment<Grunnbeløp> segment2024 = snittTidslinje.toSegments().stream()
            .filter(s -> s.getFom().getYear() == 2024)
            .findFirst()
            .orElseThrow();

        // Fra Jan-Apr (4 måneder) = 118620, fra Mai-Des (8 måneder) = 124 028
        // Vektet snitt = (118620 * 4 + 124028 * 8) / 12 = 122 225
        BigDecimal forventetSnitt = new BigDecimal(122_225);
        BigDecimal faktiskSnitt = segment2024.getValue().verdi();

        assertThat(faktiskSnitt).isEqualByComparingTo(forventetSnitt);
    }

    @Test
    void skal_beregne_vektet_snitt_for_2025() {
        LocalDateTimeline<Grunnbeløp> snittTidslinje = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();
        LocalDateSegment<Grunnbeløp> segment2025 = snittTidslinje.toSegments().stream()
            .filter(s -> s.getFom().getYear() == 2025)
            .findFirst()
            .orElseThrow();

        // Fra Jan-Apr (4 måneder) = 124028, fra Mai-Des (8 måneder) = 130160
        // Vektet snitt = (124028 * 4 + 130160 * 8) / 12 = 128 116
        BigDecimal forventetSnitt = new BigDecimal(128_116);
        BigDecimal faktiskSnitt = segment2025.getValue().verdi();

        assertThat(faktiskSnitt).isEqualByComparingTo(forventetSnitt);
    }

    @Test
    void skal_ha_oppjusteringsfaktor_1_for_siste_år() {
        LocalDateTimeline<BigDecimal> oppjusteringsTidslinje = GrunnbeløpTidslinje.lagInflasjonsfaktorTidslinje(Year.of(2025), 2);

        LocalDateSegment<BigDecimal> inflasjonsfaktorForSisteÅr = oppjusteringsTidslinje.toSegments().stream()
            .filter(s -> s.getFom().getYear() == 2025)
            .findFirst().orElseThrow();

        assertThat(BigDecimal.ONE).isEqualByComparingTo(inflasjonsfaktorForSisteÅr.getValue());
    }

    @Test
    void skal_ha_oppjusteringsfaktor_for_dette_året_og_siste_tre_år() {
        LocalDateTimeline<BigDecimal> oppjusteringsTidslinje = GrunnbeløpTidslinje.lagInflasjonsfaktorTidslinje(Year.of(2026), 3);
        for (LocalDateSegment<BigDecimal> segment : oppjusteringsTidslinje.toSegments()) {
            int år = segment.getFom().getYear();

            var verdi = segment.getValue();
            switch (år) {
                case 2023 -> assertThat(verdi).isEqualByComparingTo(BigDecimal.valueOf(1.1197618699));
                case 2024 -> assertThat(verdi).isEqualByComparingTo(BigDecimal.valueOf(1.0649212518));
                case 2025 -> assertThat(verdi).isEqualByComparingTo(BigDecimal.valueOf(1.0159542914));
                case 2026 -> assertThat(verdi).isEqualByComparingTo(BigDecimal.ONE);
            }
        }

        long antallTidligereÅr = oppjusteringsTidslinje.toSegments().stream()
            .filter(s -> s.getFom().getYear() < 2026)
            .count();

        assertThat(antallTidligereÅr).isEqualTo(3);
    }
}


