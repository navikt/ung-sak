package no.nav.ung.sak.grunnbeløp;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

class GrunnbeløpTidslinjeTest {

    @Test
    void skal_returnere_vektet_gjennomsnittlig_grunnbeløp_per_år() {
        LocalDateTimeline<Grunnbeløp> snittTidslinje = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();

        for (LocalDateSegment<Grunnbeløp> segment : snittTidslinje.toSegments()) {
            int år = Year.of(segment.getFom().getYear()).getValue();

            assertEquals(LocalDate.of(år, 1, 1), segment.getFom());
            assertEquals(LocalDate.of(år, 12, 31), segment.getTom());

            assertTrue(segment.getValue().verdi().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    void skal_beregne_korrekt_vektet_snitt_for_2024() {
        LocalDateTimeline<Grunnbeløp> snittTidslinje = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();

        LocalDateSegment<Grunnbeløp> segment2024 = snittTidslinje.toSegments().stream()
            .filter(s -> s.getFom().getYear() == 2024)
            .findFirst()
            .orElseThrow();

        // Fra Jan-Apr (4 måneder) = 118620, fra Mai-Des (8 måneder) = 124028
        // Vektet snitt = (118620 * 4 + 124028 * 8) / 12 = 122225
        BigDecimal forventetSnitt = new BigDecimal("122225");
        BigDecimal faktiskSnitt = segment2024.getValue().verdi();

        // Tillat liten avvik for avrunding
        assertTrue(faktiskSnitt.subtract(forventetSnitt).abs().compareTo(BigDecimal.valueOf(0.01)) <= 0,
            "Forventet ca " + forventetSnitt + " men fikk " + faktiskSnitt);
    }

    @Test
    void skal_beregne_korrekt_vektet_snitt_for_2025() {
        LocalDateTimeline<Grunnbeløp> snittTidslinje = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();
        LocalDateSegment<Grunnbeløp> segment2025 = snittTidslinje.toSegments().stream()
            .filter(s -> s.getFom().getYear() == 2025)
            .findFirst()
            .orElseThrow();

        // Fra Jan-Apr (4 måneder) = 124028, fra Mai-Des (8 måneder) = 130160
        // Vektet snitt = (124028 * 4 + 130160 * 8) / 12 = 128116.00
        BigDecimal forventetSnitt = new BigDecimal("128116.00");
        BigDecimal faktiskSnitt = segment2025.getValue().verdi();

        // Tillat liten avvik for avrunding
        assertTrue(faktiskSnitt.subtract(forventetSnitt).abs().compareTo(BigDecimal.valueOf(0.01)) <= 0,
            "Forventet ca " + forventetSnitt + " men fikk " + faktiskSnitt);
    }

    @Test
    void skal_ha_oppjusteringsfaktor_1_for_siste_år() {
        LocalDateTimeline<BigDecimal> oppjusteringsTidslinje = GrunnbeløpTidslinje.lagInflasjonsfaktorTidslinje(Year.of(2025), 2);

        LocalDateSegment<BigDecimal> inflasjonsfaktorForSisteÅr = oppjusteringsTidslinje.toSegments().stream()
            .filter(s -> s.getFom().getYear() == 2025)
            .findFirst().orElseThrow();

        assertEquals(BigDecimal.ONE.setScale(10, BigDecimal.ROUND_HALF_EVEN), inflasjonsfaktorForSisteÅr.getValue(),
            "Inflasjonsfaktoren for siste året skal være 1.0");
    }

    @Test
    void skal_ha_oppjusteringsfaktor_over_1_for_tidligere_år() {
        LocalDateTimeline<BigDecimal> oppjusteringsTidslinje = GrunnbeløpTidslinje.lagInflasjonsfaktorTidslinje(Year.of(2024), 2);

        // Hent segmenter for tidligere år (2023 og 2024)
        for (LocalDateSegment<BigDecimal> segment : oppjusteringsTidslinje.toSegments()) {
            int år = segment.getFom().getYear();

            if (år < 2024) {
                assertTrue(segment.getValue().compareTo(BigDecimal.ONE) > 0,
                    "Oppjusteringsfaktoren for år " + år + " skal være større enn 1.0, men var " + segment.getValue());
            }
        }

        long antallTidligereÅr = oppjusteringsTidslinje.toSegments().stream()
            .filter(s -> s.getFom().getYear() < 2025)
            .count();

        assertTrue(antallTidligereÅr > 0, "Det skal være minst ett tidligere år i tidslinjen");
    }
}


