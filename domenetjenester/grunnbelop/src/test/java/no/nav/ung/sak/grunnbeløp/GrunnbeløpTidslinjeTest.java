package no.nav.ung.sak.grunnbeløp;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GrunnbeløpSnittTidslinjeTest {

    @Test
    void skal_ha_oppjusteringsfaktor_1_for_siste_år() {
        LocalDateTimeline<BigDecimal> oppjusteringsTidslinje = GrunnbeløpSnittTidslinje.lagOppjusteringsfaktorTidslinje(Year.of(2025), 2);

        LocalDateSegment<BigDecimal> oppjusteringsfaktorForSisteÅr = oppjusteringsTidslinje.toSegments().stream()
            .filter(s -> s.getFom().getYear() == 2025)
            .findFirst().orElseThrow();

        assertThat(BigDecimal.ONE).isEqualByComparingTo(oppjusteringsfaktorForSisteÅr.getValue());
    }

    @Test
    void skal_ha_oppjusteringsfaktor_for_dette_året_og_siste_tre_år() {
        LocalDateTimeline<BigDecimal> oppjusteringsTidslinje = GrunnbeløpSnittTidslinje.lagOppjusteringsfaktorTidslinje(Year.of(2026), 3);
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


