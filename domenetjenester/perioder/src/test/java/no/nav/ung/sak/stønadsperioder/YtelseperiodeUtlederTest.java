package no.nav.ung.sak.stønadsperioder;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;
import no.nav.ung.sak.ytelseperioder.YtelsesperiodeDefinisjon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class YtelseperiodeUtlederTest {

    private YtelseperiodeUtleder ytelseperiodeUtleder;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = mock(UngdomsprogramPeriodeTjeneste.class);

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;


    @BeforeEach
    void setUp() {

        ytelseperiodeUtleder = new YtelseperiodeUtleder(ungdomsprogramPeriodeTjeneste, behandlingRepository);
    }

    @Test
    void testUngdomsprogramUtenSluttdato() {
        LocalDate startDate = LocalDate.of(2023, 1, 15);
        final var fagsakTom = startDate.plusMonths(1);
        Long behandlingId = lagFagsakOgBehandling(startDate, fagsakTom);
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, TIDENES_ENDE, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        var result = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);

        List<LocalDateSegment<YtelsesperiodeDefinisjon>> expectedSegments = List.of(
            new LocalDateSegment<>(startDate, startDate.with(TemporalAdjusters.lastDayOfMonth()), YtelsesperiodeDefinisjon.fraFomDato(startDate)),
            new LocalDateSegment<>(startDate.plusMonths(1).withDayOfMonth(1), fagsakTom, YtelsesperiodeDefinisjon.fraFomDato(startDate.plusMonths(1)))
            );
        LocalDateTimeline<YtelsesperiodeDefinisjon> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

    @Test
    void testUngdomsprogramStarterOgSlutterMidtIMåneden() {
        LocalDate startDate = LocalDate.of(2023, 1, 15);
        LocalDate endDate = LocalDate.of(2023, 1, 20);
        Long behandlingId = lagFagsakOgBehandling(startDate, startDate.plusWeeks(52).minusDays(1));
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        var result = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);

        List<LocalDateSegment<YtelsesperiodeDefinisjon>> expectedSegments = List.of(new LocalDateSegment<>(startDate, endDate, YtelsesperiodeDefinisjon.fraFomDato(startDate)));
        LocalDateTimeline<YtelsesperiodeDefinisjon> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }


    @Test
    void testUngdomsprogramStarterIBegynnelsenOgSlutterIMidten() {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 15);
        Long behandlingId = lagFagsakOgBehandling(startDate, startDate.plusWeeks(52).minusDays(1));
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<YtelsesperiodeDefinisjon> result = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);

        List<LocalDateSegment<YtelsesperiodeDefinisjon>> expectedSegments = List.of(new LocalDateSegment<>(startDate, endDate, YtelsesperiodeDefinisjon.fraFomDato(startDate)));
        LocalDateTimeline<YtelsesperiodeDefinisjon> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

    @Test
    void testUngdomsprogramStarterIMidtenAvMånedenOgSlutterIMidtenAvNesteMåned() {
        LocalDate startDate = LocalDate.of(2023, 1, 15);
        LocalDate endDate = LocalDate.of(2023, 2, 15);
        Long behandlingId = lagFagsakOgBehandling(startDate, startDate.plusWeeks(52).minusDays(1));
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<YtelsesperiodeDefinisjon> result = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);

        List<LocalDateSegment<YtelsesperiodeDefinisjon>> expectedSegments = List.of(
            new LocalDateSegment<>(startDate, LocalDate.of(2023, 1, 31), new YtelsesperiodeDefinisjon("JANUARY")),
            new LocalDateSegment<>(LocalDate.of(2023, 2, 1), endDate, new YtelsesperiodeDefinisjon("FEBRUARY"))
        );
        LocalDateTimeline<YtelsesperiodeDefinisjon> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

    @Test
    void testUngdomsprogramStarterIBegynnelsenAvMånedenOgSlutterVedSluttenAvNesteMåned() {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 2, 28);
        Long behandlingId = lagFagsakOgBehandling(startDate, startDate.plusWeeks(52).minusDays(1));
        LocalDateTimeline<Boolean> mockedTimeline = new LocalDateTimeline<>(startDate, endDate, true);
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)).thenReturn(mockedTimeline);

        LocalDateTimeline<YtelsesperiodeDefinisjon> result = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);

        List<LocalDateSegment<YtelsesperiodeDefinisjon>> expectedSegments = List.of(
            new LocalDateSegment<>(startDate, LocalDate.of(2023, 1, 31), new YtelsesperiodeDefinisjon("JANUARY")),
            new LocalDateSegment<>(LocalDate.of(2023, 2, 1), endDate, new YtelsesperiodeDefinisjon("FEBRUARY"))
        );
        LocalDateTimeline<YtelsesperiodeDefinisjon> expectedTimeline = new LocalDateTimeline<>(expectedSegments);

        assertEquals(expectedTimeline, result);
    }

    private Long lagFagsakOgBehandling(LocalDate fagsakFom, LocalDate fagsakTom) {
        final var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fagsakFom, fagsakTom);
        fagsakRepository.opprettNy(fagsak);
        final var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling.getId();
    }

}
