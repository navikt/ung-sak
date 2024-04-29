package no.nav.k9.sak.domene.typer.tid;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class TidslinjeUtilTest {

    @Test
    void skal_splitte_og_gruppere_på_år() {
        assertThat(TidslinjeUtil.splittOgGruperPåÅrstall(LocalDateTimeline.empty())).isEmpty();

        LocalDateTimeline<Boolean> tidslinje2022 = new LocalDateTimeline<>(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), true);
        assertThat(TidslinjeUtil.splittOgGruperPåÅrstall(tidslinje2022)).isEqualTo(Map.of(Year.of(2022), tidslinje2022));


        LocalDate d_2021_12_31 = LocalDate.of(2021, 12, 31);
        LocalDate d_2023_01_01 = LocalDate.of(2023, 1, 1);
        assertThat(TidslinjeUtil.splittOgGruperPåÅrstall(new LocalDateTimeline<>(d_2021_12_31, d_2023_01_01, true))).isEqualTo(Map.of(
            Year.of(2021), new LocalDateTimeline<>(d_2021_12_31, d_2021_12_31, true),
            Year.of(2022), tidslinje2022,
            Year.of(2023), new LocalDateTimeline<>(d_2023_01_01, d_2023_01_01, true)
        ));
    }

    @Test
    void name() {
        Optional<Integer> v = Optional.of(20);
        Optional<Object> x = v.map(z -> null);
        System.out.println(x.isPresent());
    }

    @Test
    void skal_begrense_til_antall_dager() {
        LocalDate mandag_uke1 = LocalDate.of(2022, 1, 3);
        LocalDate fredag_uke1 = LocalDate.of(2022, 1, 7);
        LocalDate søndag_uke1 = LocalDate.of(2022, 1, 9);
        LocalDate mandag_uke2 = LocalDate.of(2022, 1, 10);
        LocalDate fredag_uke2 = LocalDate.of(2022, 1, 14);
        LocalDate søndag_uke2 = LocalDate.of(2022, 1, 16);

        //test av tom tidslinje
        assertThat(TidslinjeUtil.begrensTilAntallDager(LocalDateTimeline.empty(), 1000, false)).isEqualTo(LocalDateTimeline.empty());

        //test av kontinuerlig tidslinje
        LocalDateTimeline<Boolean> tidslinje1 = new LocalDateTimeline<>(mandag_uke1, søndag_uke2, true);
        assertThat(TidslinjeUtil.begrensTilAntallDager(tidslinje1, 0, false)).isEqualTo(LocalDateTimeline.empty());

        assertThat(TidslinjeUtil.begrensTilAntallDager(tidslinje1, 1, false)).isEqualTo(new LocalDateTimeline<>(mandag_uke1, mandag_uke1, true));
        assertThat(TidslinjeUtil.begrensTilAntallDager(tidslinje1, 6, false)).isEqualTo(new LocalDateTimeline<>(mandag_uke1, mandag_uke2, true));
        assertThat(TidslinjeUtil.begrensTilAntallDager(tidslinje1, 7, true)).isEqualTo(new LocalDateTimeline<>(mandag_uke1, søndag_uke1, true));

        //test av tidslinje med hull
        LocalDateTimeline<Boolean> tidslinje2 = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(mandag_uke1, fredag_uke1, true),
            new LocalDateSegment<>(mandag_uke2, fredag_uke2, true)
        ));
        assertThat(TidslinjeUtil.begrensTilAntallDager(tidslinje2, 10, false)).isEqualTo(tidslinje2);
        assertThat(TidslinjeUtil.begrensTilAntallDager(tidslinje2, 6, false)).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(mandag_uke1, fredag_uke1, true),
            new LocalDateSegment<>(mandag_uke2, mandag_uke2, true)
        )));
    }

    @Test
    void skal_utlede_forskjell_for_sett() {
        LocalDate dag1 = LocalDate.now();
        LocalDate dag2 = dag1.plusDays(1);
        LocalDate dag3 = dag1.plusDays(2);
        LocalDateTimeline<Set<String>> tidslinjeA = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag1, dag1, Set.of("A")),
            new LocalDateSegment<>(dag2, dag2, Set.of("A", "B")),
            new LocalDateSegment<>(dag3, dag3, Set.of("A", "C")))
        );
        LocalDateTimeline<Set<String>> tidslinjeB = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag2, dag2, Set.of("A", "B")),
            new LocalDateSegment<>(dag3, dag3, Set.of("A")))
        );

        LocalDateTimeline<Set<String>> resultat = tidslinjeA.combine(tidslinjeB, TidslinjeUtil::forskjell, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        Assertions.assertThat(resultat)
            .isEqualTo(new LocalDateTimeline<>(List.of(
                    new LocalDateSegment<>(dag1, dag1, Set.of("A")),
                    new LocalDateSegment<>(dag3, dag3, Set.of("C")))
                )
            );


        Assertions.assertThat(new LocalDateTimeline<>(dag1, dag1, Set.of()).combine(new LocalDateTimeline<>(dag2, dag2, Set.of()), TidslinjeUtil::forskjell, LocalDateTimeline.JoinStyle.CROSS_JOIN)).isEqualTo(LocalDateTimeline.EMPTY_TIMELINE);
    }
}
