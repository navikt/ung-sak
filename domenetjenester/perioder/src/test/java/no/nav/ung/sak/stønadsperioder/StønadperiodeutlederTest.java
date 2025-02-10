package no.nav.ung.sak.stønadsperioder;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StønadperiodeutlederTest {

    private Stønadperiodeutleder stønadperiodeutleder;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = mock(UngdomsprogramPeriodeTjeneste.class);

    @BeforeEach
    void setUp() {
        stønadperiodeutleder = new Stønadperiodeutleder(ungdomsprogramPeriodeTjeneste);
    }

    @Test
    void testUngdomsprogramStarterOgSlutterMidtIMåneden() {
        Long behandlingId = 1L;
        LocalDate startDate = LocalDate.of(2023, 1, 15);
        LocalDate endDate = LocalDate.of(2023, 1, 20);
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<Boolean> result = stønadperiodeutleder.utledStønadstidslinje(behandlingId);

        List<LocalDateSegment<Boolean>> expectedSegments = List.of(new LocalDateSegment<>(startDate, endDate, true));
        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }


    @Test
    void testUngdomsprogramStarterIBegynnelsenOgSlutterIMidten() {
        Long behandlingId = 1L;
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 15);
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<Boolean> result = stønadperiodeutleder.utledStønadstidslinje(behandlingId);

        List<LocalDateSegment<Boolean>> expectedSegments = List.of(new LocalDateSegment<>(startDate, endDate, true));
        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

    @Test
    void testUngdomsprogramStarterIMidtenAvMånedenOgSlutterIMidtenAvNesteMåned() {
        Long behandlingId = 1L;
        LocalDate startDate = LocalDate.of(2023, 1, 15);
        LocalDate endDate = LocalDate.of(2023, 2, 15);
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<Boolean> result = stønadperiodeutleder.utledStønadstidslinje(behandlingId);

        List<LocalDateSegment<Boolean>> expectedSegments = List.of(
            new LocalDateSegment<>(startDate, LocalDate.of(2023, 1, 31), true),
            new LocalDateSegment<>(LocalDate.of(2023, 2, 1), endDate, true)
        );
        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

    @Test
    void testUngdomsprogramStarterIBegynnelsenAvMånedenOgSlutterVedSluttenAvNesteMåned() {
        Long behandlingId = 1L;
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 2, 28);
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<Boolean> result = stønadperiodeutleder.utledStønadstidslinje(behandlingId);

        List<LocalDateSegment<Boolean>> expectedSegments = List.of(
            new LocalDateSegment<>(startDate, LocalDate.of(2023, 1, 31), true),
            new LocalDateSegment<>(LocalDate.of(2023, 2, 1), endDate, true)
        );
        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

}
