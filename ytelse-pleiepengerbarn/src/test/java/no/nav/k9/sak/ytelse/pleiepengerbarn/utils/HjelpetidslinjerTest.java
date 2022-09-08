package no.nav.k9.sak.ytelse.pleiepengerbarn.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class HjelpetidslinjerTest {

    @Test
    public void helgelinjeTom() {
        final LocalDate fom = LocalDate.of(2022, 8, 2);
        final LocalDate tom = LocalDate.of(2022, 8, 4);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ha_med_helg_mellom_fredag_og_mandag() {
        final LocalDate fredag = LocalDate.of(2022, 8, 5);
        final LocalDate mandag = LocalDate.of(2022, 8, 8);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fredag, fredag, true),
            new LocalDateSegment<>(mandag, mandag, true) )
        ));
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7)
        );
    }

    @Test
    public void helgetidslinjeMedHelgerBeggeKanter() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 5);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7)
        );
    }

    @Test
    public void helgetidslinjeMedTorsdag() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 4);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31)
        );
    }
    
    @Test
    public void helgetidslinjeMedStartPåSøndagOgSluttPåLørdag() {
        final LocalDate fom = LocalDate.of(2022, 8, 7);
        final LocalDate tom = LocalDate.of(2022, 8, 13);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14)
        );
    }

    @Test
    public void helgetidslinjeMedLangPeriode() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 26);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14),
            LocalDate.of(2022, 8, 20), LocalDate.of(2022, 8, 21),
            LocalDate.of(2022, 8, 27), LocalDate.of(2022, 8, 28)
        );
    }

    @Test
    public void helgetidslinjeMedFlerePerioder() {
        final LocalDateTimeline<Boolean> tidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 12), Boolean.TRUE),
            new LocalDateSegment<>(LocalDate.of(2022, 8, 15), LocalDate.of(2022, 8, 26), Boolean.TRUE)
        ));

        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(tidslinje);
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14),
            LocalDate.of(2022, 8, 20), LocalDate.of(2022, 8, 21),
            LocalDate.of(2022, 8, 27), LocalDate.of(2022, 8, 28)
        );
    }
    
    @Test
    public void helgetidslinjeMedSammenhengendePeriodeSomStarterPåFredag() {
        final LocalDateTimeline<Boolean> tidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 4), Boolean.TRUE),
            new LocalDateSegment<>(LocalDate.of(2022, 8, 5), LocalDate.of(2022, 8, 12), Boolean.TRUE)
        ));

        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(tidslinje);
        System.out.println(resultat);
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14)
        );
    }
    
    @Test
    public void helgetidslinjeSomStarterPåTorsdag() {
        final LocalDateTimeline<Boolean> tidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 3), Boolean.TRUE),
            new LocalDateSegment<>(LocalDate.of(2022, 8, 4), LocalDate.of(2022, 8, 11), Boolean.TRUE),
            new LocalDateSegment<>(LocalDate.of(2022, 8, 15), LocalDate.of(2022, 8, 19), Boolean.TRUE),
            new LocalDateSegment<>(LocalDate.of(2022, 8, 22), LocalDate.of(2022, 8, 26), Boolean.TRUE)
        ));

        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(tidslinje);
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14),
            LocalDate.of(2022, 8, 20), LocalDate.of(2022, 8, 21),
            LocalDate.of(2022, 8, 27), LocalDate.of(2022, 8, 28)
        );
    }

    @Test
    public void helgetidslinjeMedFlerePerioderKantIKant() {
        final LocalDateTimeline<Boolean> tidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 12), Boolean.TRUE),
            new LocalDateSegment<>(LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 26), Boolean.TRUE)
        ));

        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(tidslinje);
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14),
            LocalDate.of(2022, 8, 20), LocalDate.of(2022, 8, 21),
            LocalDate.of(2022, 8, 27), LocalDate.of(2022, 8, 28)
        );
    }

    private <V> void assertTidslinjeInneholder(LocalDateTimeline<V> resultat, LocalDate... datoer) {
        System.out.println(resultat);
        final var segments = resultat.toSegments();
        assertThat(segments.size()).isEqualTo(datoer.length / 2);
        int index = 0;
        for (LocalDateSegment<V> segment : segments) {
            assertThat(segment.getFom()).isEqualTo(datoer[index]);
            index++;
            assertThat(segment.getTom()).isEqualTo(datoer[index]);
            index++;
        }
    }
}
