package no.nav.ung.sak.ytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelevanteKontrollperioderUtlederTest {

    @Test
    void skal_finne_første_og_siste_periode_for_sammenhengende_ytelsestidslinje_med_tre_segmenter() {
        final var førstePeriodeFom = LocalDate.of(2023, 1, 12);
        final var førstePeriodeTom = LocalDate.of(2023, 1, 31);
        final var sistePeriodeFom = LocalDate.of(2023, 3, 1);
        final var sistePeriodeTom = LocalDate.of(2023, 3, 14);
        LocalDateTimeline<YearMonth> ytelsesPerioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(førstePeriodeFom, førstePeriodeTom, YearMonth.of(2023, 1)),
            new LocalDateSegment<>(førstePeriodeTom.plusDays(1), sistePeriodeFom.minusDays(1), YearMonth.of(2023, 2)),
            new LocalDateSegment<>(sistePeriodeFom, sistePeriodeTom, YearMonth.of(2023, 3))
        ));

        LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> result = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);

        assertEquals(2, result.toSegments().size());
        final var første = result.toSegments().first();
        assertTrue(første.getValue().gjelderFørstePeriode());
        assertThat(første.getFom()).isEqualTo(førstePeriodeFom);
        assertThat(første.getTom()).isEqualTo(førstePeriodeTom);

        final var siste = result.toSegments().last();
        assertThat(siste.getFom()).isEqualTo(sistePeriodeFom);
        assertThat(siste.getTom()).isEqualTo(sistePeriodeTom);
        assertTrue(siste.getValue().gjelderSistePeriode());
    }

    @Test
    void skal_finne_første_og_siste_periode_for_sammenhengende_ytelsestidslinje_med_to_segmenter() {
        final var førstePeriodeFom = LocalDate.of(2023, 1, 13);
        final var førstePeriodeTom = LocalDate.of(2023, 1, 31);
        final var sistePeriodeFom = LocalDate.of(2023, 2, 1);
        final var sistePeriodeTom = LocalDate.of(2023, 2, 12);
        LocalDateTimeline<YearMonth> ytelsesPerioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(førstePeriodeFom, førstePeriodeTom, YearMonth.of(2023, 1)),
            new LocalDateSegment<>(sistePeriodeFom, sistePeriodeTom, YearMonth.of(2023, 2))
        ));

        LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> result = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);

        assertEquals(2, result.toSegments().size());
        final var første = result.toSegments().first();
        assertTrue(første.getValue().gjelderFørstePeriode());
        assertThat(første.getFom()).isEqualTo(førstePeriodeFom);
        assertThat(første.getTom()).isEqualTo(førstePeriodeTom);

        final var siste = result.toSegments().last();
        assertThat(siste.getFom()).isEqualTo(sistePeriodeFom);
        assertThat(siste.getTom()).isEqualTo(sistePeriodeTom);
        assertTrue(siste.getValue().gjelderSistePeriode());
    }

    @Test
    void skal_finne_første_og_siste_perioder_for_der_ytelsestidslinjen_er_delt_i_to_med_med_to_segmenter_i_hver() {
        final var førstePeriode1Fom = LocalDate.of(2023, 1, 1);
        final var førstePeriode1Tom = LocalDate.of(2023, 1, 31);
        final var sistePeriode1Fom = LocalDate.of(2023, 2, 1);
        final var sistePeriode1Tom = LocalDate.of(2023, 2, 10);

        final var førstePeriode2Fom = LocalDate.of(2023, 3, 1);
        final var førstePeriode2Tom = LocalDate.of(2023, 3, 31);
        final var sistePeriode2Fom = LocalDate.of(2023, 4, 1);
        final var sistePeriode2Tom = LocalDate.of(2023, 4, 1);
        LocalDateTimeline<YearMonth> ytelsesPerioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(førstePeriode1Fom, førstePeriode1Tom, YearMonth.of(2023, 1)),
            new LocalDateSegment<>(sistePeriode1Fom, sistePeriode1Tom, YearMonth.of(2023, 2)),
            new LocalDateSegment<>(førstePeriode2Fom, førstePeriode2Tom, YearMonth.of(2023, 3)),
            new LocalDateSegment<>(sistePeriode2Fom, sistePeriode2Tom, YearMonth.of(2023, 4))
        ));

        LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> result = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);

        assertEquals(4, result.toSegments().size());
        final var iterator = result.toSegments().iterator();
        final var første1 = iterator.next();
        assertTrue(første1.getValue().gjelderFørstePeriode());
        assertThat(første1.getFom()).isEqualTo(førstePeriode1Fom);
        assertThat(første1.getTom()).isEqualTo(førstePeriode1Tom);

        final var siste1 = iterator.next();
        assertThat(siste1.getFom()).isEqualTo(sistePeriode1Fom);
        assertThat(siste1.getTom()).isEqualTo(sistePeriode1Tom);
        assertTrue(siste1.getValue().gjelderSistePeriode());

        final var første2 = iterator.next();
        assertTrue(første2.getValue().gjelderFørstePeriode());
        assertThat(første2.getFom()).isEqualTo(førstePeriode2Fom);
        assertThat(første2.getTom()).isEqualTo(førstePeriode2Tom);

        final var siste2 = iterator.next();
        assertThat(siste2.getFom()).isEqualTo(sistePeriode2Fom);
        assertThat(siste2.getTom()).isEqualTo(sistePeriode2Tom);
        assertTrue(siste2.getValue().gjelderSistePeriode());
    }

    @Test
    void skal_finne_første_og_siste_perioder_for_der_ytelsestidslinjen_er_delt_i_to_med_med_tre_segmenter_i_hver() {
        final var førstePeriode1Fom = LocalDate.of(2023, 1, 1);
        final var førstePeriode1Tom = LocalDate.of(2023, 1, 31);
        final var sistePeriode1Fom = LocalDate.of(2023, 3, 1);
        final var sistePeriode1Tom = LocalDate.of(2023, 3, 10);

        final var førstePeriode2Fom = LocalDate.of(2023, 4, 1);
        final var førstePeriode2Tom = LocalDate.of(2023, 4, 30);
        final var sistePeriode2Fom = LocalDate.of(2023, 6, 1);
        final var sistePeriode2Tom = LocalDate.of(2023, 6, 2);
        LocalDateTimeline<YearMonth> ytelsesPerioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(førstePeriode1Fom, førstePeriode1Tom, YearMonth.of(2023, 1)),
            new LocalDateSegment<>(førstePeriode1Tom.plusDays(1), sistePeriode1Fom.minusDays(1), YearMonth.of(2023, 2)),
            new LocalDateSegment<>(sistePeriode1Fom, sistePeriode1Tom, YearMonth.of(2023, 3)),
            new LocalDateSegment<>(førstePeriode2Fom, førstePeriode2Tom, YearMonth.of(2023, 4)),
            new LocalDateSegment<>(førstePeriode2Tom.plusDays(1), sistePeriode2Fom.minusDays(1), YearMonth.of(2023, 5)),
            new LocalDateSegment<>(sistePeriode2Fom, sistePeriode2Tom, YearMonth.of(2023, 6))
        ));

        LocalDateTimeline<RelevanteKontrollperioderUtleder.FritattForKontroll> result = RelevanteKontrollperioderUtleder.finnPerioderDerKontrollIkkeErPåkrevd(ytelsesPerioder);

        assertEquals(4, result.toSegments().size());
        final var iterator = result.toSegments().iterator();
        final var første1 = iterator.next();
        assertTrue(første1.getValue().gjelderFørstePeriode());
        assertThat(første1.getFom()).isEqualTo(førstePeriode1Fom);
        assertThat(første1.getTom()).isEqualTo(førstePeriode1Tom);

        final var siste1 = iterator.next();
        assertThat(siste1.getFom()).isEqualTo(sistePeriode1Fom);
        assertThat(siste1.getTom()).isEqualTo(sistePeriode1Tom);
        assertTrue(siste1.getValue().gjelderSistePeriode());

        final var første2 = iterator.next();
        assertTrue(første2.getValue().gjelderFørstePeriode());
        assertThat(første2.getFom()).isEqualTo(førstePeriode2Fom);
        assertThat(første2.getTom()).isEqualTo(førstePeriode2Tom);

        final var siste2 = iterator.next();
        assertThat(siste2.getFom()).isEqualTo(sistePeriode2Fom);
        assertThat(siste2.getTom()).isEqualTo(sistePeriode2Tom);
        assertTrue(siste2.getValue().gjelderSistePeriode());
    }

}
