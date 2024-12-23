package no.nav.ung.sak.domene.typer.tid;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;

public class HjelpetidslinjerTest {

    @Test
    public void skal_utlede_helg_som_må_tettes() {
        final LocalDate fredag = LocalDate.of(2022, 8, 5);
        final LocalDate mandag = LocalDate.of(2022, 8, 8);
        var tidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fredag, fredag, true),
            new LocalDateSegment<>(mandag, mandag, true))
        );
        var hullSomMåTettes = Hjelpetidslinjer.utledHullSomMåTettes(tidslinje, new PåTversAvHelgErKantIKantVurderer());
        var segmenter = hullSomMåTettes.toSegments();
        Assertions.assertThat(segmenter.size()).isEqualTo(1);
        Assertions.assertThat(segmenter.getFirst().getFom()).isEqualTo(fredag.plusDays(1));
        Assertions.assertThat(segmenter.getFirst().getTom()).isEqualTo(mandag.minusDays(1));
    }

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
        Assertions.assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    public void helgetidslinjeMedHelgerBeggeKanter() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 5);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    public void helgetidslinjeFungererMedFomAlleDager() {
        for (int fomUkedag=1; fomUkedag <= 5; fomUkedag++) {
            final LocalDate fom = LocalDate.of(2022, 8, fomUkedag);
            final LocalDate tom = LocalDate.of(2022, 8, 7);
            final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
            assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7)
            );
        }

        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 7);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7)
        );
    }

    @Test
    public void helgetidslinjeFungererMedTomAlleDager() {
        for (int i=7; i <= 12; i++) {
            final LocalDate fom = LocalDate.of(2022, 8, 5);
            final LocalDate tom = LocalDate.of(2022, 8, i);
            final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
            assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7)
            );
        }

        final LocalDate fom = LocalDate.of(2022, 8, 5);
        final LocalDate tom = LocalDate.of(2022, 8, 13);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 13)
        );
    }

    @Test
    public void helgetidslinjeMedTorsdag() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 4);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    public void helgetidslinjeMedStartPåSøndagOgSluttPåLørdag() {
        final LocalDate fom = LocalDate.of(2022, 8, 7);
        final LocalDate tom = LocalDate.of(2022, 8, 13);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertTidslinjeInneholder(resultat,
            LocalDate.of(2022, 8, 7), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 13)
        );
    }

    @Test
    public void helgetidslinjeMedLangPeriode() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 26);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagTidslinjeMedKunHelger(new LocalDateTimeline<>(fom, tom, Boolean.TRUE));
        assertTidslinjeInneholder(resultat,
//            LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14),
            LocalDate.of(2022, 8, 20), LocalDate.of(2022, 8, 21)
//            LocalDate.of(2022, 8, 27), LocalDate.of(2022, 8, 28)
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
            //LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            //LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14),
            LocalDate.of(2022, 8, 20), LocalDate.of(2022, 8, 21)
            //LocalDate.of(2022, 8, 27), LocalDate.of(2022, 8, 28)
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
//            LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7)
  //          LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14)
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
            //LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7)
            //LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14),
            //LocalDate.of(2022, 8, 20), LocalDate.of(2022, 8, 21),
            //LocalDate.of(2022, 8, 27), LocalDate.of(2022, 8, 28)
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
            //LocalDate.of(2022, 7, 30), LocalDate.of(2022, 7, 31),
            LocalDate.of(2022, 8, 6), LocalDate.of(2022, 8, 7),
            LocalDate.of(2022, 8, 13), LocalDate.of(2022, 8, 14),
            LocalDate.of(2022, 8, 20), LocalDate.of(2022, 8, 21)
            //LocalDate.of(2022, 8, 27), LocalDate.of(2022, 8, 28)
        );
    }


    @Test
    public void ukestidslinjeTom() {
        final LocalDate fom = LocalDate.of(2022, 8, 6);
        final LocalDate tom = LocalDate.of(2022, 8, 7);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void ukestidslinjeSkalIkkeHaMedLørdagISlutten() {
        final LocalDate fom = LocalDate.of(2022, 8, 2);
        final LocalDate tom = LocalDate.of(2022, 8, 6);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 2), LocalDate.of(2022, 8, 5)
                );
    }

    @Test
    public void ukestidslinjeMandagTilTorsdag() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 4);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 4)
                );
    }

    @Test
    public void ukestidslinjeMandagTilTorsdagNesteUke() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 11);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 5),
                LocalDate.of(2022, 8, 8), LocalDate.of(2022, 8, 11)
                );
    }

    @Test
    public void ukestidslinjeSkalIkkeHaMedSøndagISlutten() {
        final LocalDate fom = LocalDate.of(2022, 8, 2);
        final LocalDate tom = LocalDate.of(2022, 8, 7);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 2), LocalDate.of(2022, 8, 5)
                );
    }

    @Test
    public void ukestidslinjeSkalIkkeHaMedLørdagIStarten() {
        final LocalDate fom = LocalDate.of(2022, 8, 6);
        final LocalDate tom = LocalDate.of(2022, 8, 14);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 8), LocalDate.of(2022, 8, 12)
                );
    }

    @Test
    public void ukestidslinjeSkalIkkeHaMedSøndagIStarten() {
        final LocalDate fom = LocalDate.of(2022, 8, 7);
        final LocalDate tom = LocalDate.of(2022, 8, 14);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 8), LocalDate.of(2022, 8, 12)
                );
    }

    @Test
    public void ukestidslinjeMandagTilFredag() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 5);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 5)
                );
    }

    @Test
    public void ukestidslinjeSøndagTilFredag() {
        final LocalDate fom = LocalDate.of(2022, 7, 31);
        final LocalDate tom = LocalDate.of(2022, 8, 5);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 5)
                );
    }

    @Test
    public void ukestidslinjeMandagTilMandag() {
        final LocalDate fom = LocalDate.of(2022, 8, 1);
        final LocalDate tom = LocalDate.of(2022, 8, 8);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 5),
                LocalDate.of(2022, 8, 8), LocalDate.of(2022, 8, 8)
                );
    }

    @Test
    public void ukestidslinjeFredagTilMandag() {
        final LocalDate fom = LocalDate.of(2022, 8, 5);
        final LocalDate tom = LocalDate.of(2022, 8, 8);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 5), LocalDate.of(2022, 8, 5),
                LocalDate.of(2022, 8, 8), LocalDate.of(2022, 8, 8)
                );
    }

    @Test
    public void ukestidslinjeTirsdagOgTreUkerTilOnsdag() {
        final LocalDate fom = LocalDate.of(2022, 8, 2);
        final LocalDate tom = LocalDate.of(2022, 8, 24);
        final LocalDateTimeline<Boolean> resultat = Hjelpetidslinjer.lagUkestidslinjeForMandagTilFredag(fom, tom);
        assertTidslinjeInneholder(resultat,
                LocalDate.of(2022, 8, 2), LocalDate.of(2022, 8, 5),
                LocalDate.of(2022, 8, 8), LocalDate.of(2022, 8, 12),
                LocalDate.of(2022, 8, 15), LocalDate.of(2022, 8, 19),
                LocalDate.of(2022, 8, 22), LocalDate.of(2022, 8, 24)
                );
    }

    private <V> void assertTidslinjeInneholder(LocalDateTimeline<V> resultat, LocalDate... datoer) {
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
