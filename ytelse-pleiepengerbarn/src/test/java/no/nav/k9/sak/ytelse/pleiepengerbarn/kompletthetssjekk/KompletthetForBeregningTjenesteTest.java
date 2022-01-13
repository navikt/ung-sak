package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class KompletthetForBeregningTjenesteTest {

    private KompletthetForBeregningTjeneste sjekker = new KompletthetForBeregningTjeneste();

    @Test
    void skal_utlede_relevant_periode_ved_ingen_overlapp() {
        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(LocalDate.now().minusWeeks(6), LocalDate.now().minusWeeks(5), true), new LocalDateSegment<>(LocalDate.now(), LocalDate.now().plusWeeks(1), true)));

        var relevantPeriode = sjekker.utledRelevantPeriode(tidslinje, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusWeeks(1)));

        assertThat(relevantPeriode).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusWeeks(4), LocalDate.now().plusWeeks(5)));
    }

    @Test
    void skal_utlede_relevant_periode_ved_28_dager_mellom_kanten() {
        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(LocalDate.now().minusDays(100), LocalDate.now().minusDays(90), true),
            new LocalDateSegment<>(LocalDate.now().minusDays(90-28), LocalDate.now().minusDays(90-29-30), true),
            new LocalDateSegment<>(LocalDate.now(), LocalDate.now().plusWeeks(1), true)));

        var relevantPeriode = sjekker.utledRelevantPeriode(tidslinje, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(100), LocalDate.now().minusDays(90)));

        assertThat(relevantPeriode).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(100).minusWeeks(4), LocalDate.now().minusDays(90-29-30).plusWeeks(4)));
    }

    @Test
    void skal_utlede_relevant_periode_ved_en_overlapp() {
        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(LocalDate.now().minusWeeks(6), LocalDate.now().minusWeeks(3), true), new LocalDateSegment<>(LocalDate.now(), LocalDate.now().plusWeeks(1), true)));

        var relevantPeriode = sjekker.utledRelevantPeriode(tidslinje, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusWeeks(1)));

        assertThat(relevantPeriode).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusWeeks(10), LocalDate.now().plusWeeks(5)));
    }

    @Test
    void skal_utlede_relevant_periode_ved_flere_overlapp() {
        var tidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.now().minusWeeks(10), LocalDate.now().minusWeeks(9), true),
            new LocalDateSegment<>(LocalDate.now().minusWeeks(6), LocalDate.now().minusWeeks(3), true),
            new LocalDateSegment<>(LocalDate.now(), LocalDate.now().plusWeeks(1), true),
            new LocalDateSegment<>(LocalDate.now().plusWeeks(4), LocalDate.now().plusWeeks(7), true),
            new LocalDateSegment<>(LocalDate.now().plusWeeks(10), LocalDate.now().plusWeeks(12), true)));

        var relevantPeriode = sjekker.utledRelevantPeriode(tidslinje, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusWeeks(1)));

        assertThat(relevantPeriode).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusWeeks(14), LocalDate.now().plusWeeks(16)));
    }
}
