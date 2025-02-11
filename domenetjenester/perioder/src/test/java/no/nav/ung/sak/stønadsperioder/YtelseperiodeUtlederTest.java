package no.nav.ung.sak.stønadsperioder;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class YtelseperiodeUtlederTest {

    private YtelseperiodeUtleder ytelseperiodeUtleder;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = mock(UngdomsprogramPeriodeTjeneste.class);

    @Inject
    private EntityManager entityManager;

    @Inject
    private BehandlingRepository behandlingRepository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        ytelseperiodeUtleder = new YtelseperiodeUtleder(ungdomsprogramPeriodeTjeneste, behandlingRepository);
    }

    @Test
    void testUngdomsprogramStarterOgSlutterMidtIMåneden() {
        Long behandlingId = behandling.getId();
        LocalDate startDate = LocalDate.of(2023, 1, 15);
        LocalDate endDate = LocalDate.of(2023, 1, 20);
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<Boolean> result = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);

        List<LocalDateSegment<Boolean>> expectedSegments = List.of(new LocalDateSegment<>(startDate, endDate, true));
        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }


    @Test
    void testUngdomsprogramStarterIBegynnelsenOgSlutterIMidten() {
        Long behandlingId = behandling.getId();
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 15);
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<Boolean> result = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);

        List<LocalDateSegment<Boolean>> expectedSegments = List.of(new LocalDateSegment<>(startDate, endDate, true));
        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

    @Test
    void testUngdomsprogramStarterIMidtenAvMånedenOgSlutterIMidtenAvNesteMåned() {
        Long behandlingId = behandling.getId();
        LocalDate startDate = LocalDate.of(2023, 1, 15);
        LocalDate endDate = LocalDate.of(2023, 2, 15);
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<Boolean> result = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);

        List<LocalDateSegment<Boolean>> expectedSegments = List.of(
            new LocalDateSegment<>(startDate, LocalDate.of(2023, 1, 31), true),
            new LocalDateSegment<>(LocalDate.of(2023, 2, 1), endDate, true)
        );
        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

    @Test
    void testUngdomsprogramStarterIBegynnelsenAvMånedenOgSlutterVedSluttenAvNesteMåned() {
        Long behandlingId = behandling.getId();
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 2, 28);
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<Boolean> result = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);

        List<LocalDateSegment<Boolean>> expectedSegments = List.of(
            new LocalDateSegment<>(startDate, LocalDate.of(2023, 1, 31), true),
            new LocalDateSegment<>(LocalDate.of(2023, 2, 1), endDate, true)
        );
        LocalDateTimeline<Boolean> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

}
