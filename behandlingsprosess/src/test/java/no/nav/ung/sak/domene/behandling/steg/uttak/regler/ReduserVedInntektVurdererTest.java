package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ReduserVedInntektVurdererTest {

    @Test
    void skal_returnere_ingen_perioder_ved_tom_tidslinje_til_vurdering() {

        final var fom = LocalDate.of(2025, 1, 31);
        final var tom = LocalDate.of(2025, 1, 31);
        final var resultat = new ReduserVedInntektVurderer(
            new LocalDateTimeline<>(fom, tom, Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.TEN))),
            new LocalDateTimeline<>(fom, tom, BigDecimal.TEN)
        ).vurder(LocalDateTimeline.empty());

        assertThat(resultat.resultatTidslinje().size()).isEqualTo(0);
        assertThat(resultat.restTidslinjeTilVurdering().isEmpty()).isTrue();
        assertThat(resultat.regelSporing().get("inntektdagsatsperioder")).isEqualTo(
            """
                [{
                    "dagsats": 10.00,
                    "periode": "2025-01-31/2025-01-31"
                }]"""
        );
    }


    @Test
    void skal_returnere_periode_på_en_dag_med_likt_beløp_for_rapportert_inntekt_og_dagsats() {
        // Arrange
        final var fom = LocalDate.of(2025, 1, 31);
        final var tom = LocalDate.of(2025, 1, 31);
        final var tidslinjeTilVurdering = new LocalDateTimeline<>(fom, tom, true);
        final var rapportertInntektTidslinje = new LocalDateTimeline<>(fom, tom, Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(100))));
        final var aldersbestemtSatsTidslinje = new LocalDateTimeline<>(fom, tom, BigDecimal.valueOf(100));

        // Act
        final var resultat = new ReduserVedInntektVurderer(
            rapportertInntektTidslinje,
            aldersbestemtSatsTidslinje
        ).vurder(tidslinjeTilVurdering);

        // Assert
        sammenlignTidslinjer(new LocalDateTimeline<>(fom, tom, UttakResultat.forInnvilgelse(BigDecimal.valueOf(34))), resultat.resultatTidslinje());
        assertThat(resultat.restTidslinjeTilVurdering().isEmpty()).isTrue();
        assertThat(resultat.regelSporing().get("inntektdagsatsperioder")).isEqualTo(
            """
                [{
                    "dagsats": 100.00,
                    "periode": "2025-01-31/2025-01-31"
                }]"""
        );
    }

    @Test
    void skal_returnere_uttaksperioder_for_periode_på_ti_dager_som_går_over_to_måneder_ulik_dagsats_for_rapportert_inntekt() {
        // Arrange
        final var fom = LocalDate.of(2025, 1, 31);
        final var tom = fom.plusDays(9);
        final var tidslinjeTilVurdering = new LocalDateTimeline<>(fom, tom, true);
        final var rapportertInntektTidslinje = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(fom, fom, Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(100)))),
                new LocalDateSegment<>(fom.plusDays(1), tom, Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(700))))
            ));
        final var aldersbestemtSatsTidslinje = new LocalDateTimeline<>(fom, tom, BigDecimal.valueOf(100));

        // Act
        final var resultat = new ReduserVedInntektVurderer(
            rapportertInntektTidslinje,
            aldersbestemtSatsTidslinje
        ).vurder(tidslinjeTilVurdering);

        // Assert
        final var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, fom, UttakResultat.forInnvilgelse(BigDecimal.valueOf(34))),
            new LocalDateSegment<>(fom.plusDays(1), tom, UttakResultat.forInnvilgelse(BigDecimal.valueOf(7.6)))
        ));
        sammenlignTidslinjer(forventetResultat, resultat.resultatTidslinje());
        assertThat(resultat.restTidslinjeTilVurdering().isEmpty()).isTrue();
        assertThat(resultat.regelSporing().get("inntektdagsatsperioder")).isEqualTo(
            """
                [{
                    "dagsats": 100.00,
                    "periode": "2025-01-31/2025-01-31"
                },{
                    "dagsats": 140.00,
                    "periode": "2025-02-01/2025-02-09"
                }]"""
        );
    }

    @Test
    void skal_returnere_uttaksperioder_en_uke_midt_i_en_måned_med_ny_aldersbestemt_sats() {
        // Arrange
        final var fom = LocalDate.of(2025, 2, 3);
        final var tom = fom.plusDays(4);
        final var tidslinjeTilVurdering = new LocalDateTimeline<>(fom, tom, true);
        final var rapportertInntektTidslinje = new LocalDateTimeline<>(fom, tom, Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(500))));
        final var aldersbestemtSatsTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, fom, BigDecimal.valueOf(100)),
            new LocalDateSegment<>(fom.plusDays(1), fom.plusDays(100), BigDecimal.valueOf(150))
            ));

        // Act
        final var resultat = new ReduserVedInntektVurderer(
            rapportertInntektTidslinje,
            aldersbestemtSatsTidslinje
        ).vurder(tidslinjeTilVurdering);

        // Assert
        final var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, fom, UttakResultat.forInnvilgelse(BigDecimal.valueOf(34))),
            new LocalDateSegment<>(fom.plusDays(1), tom, UttakResultat.forInnvilgelse(BigDecimal.valueOf(56)))
        ));
        sammenlignTidslinjer(forventetResultat, resultat.resultatTidslinje());
        assertThat(resultat.restTidslinjeTilVurdering().isEmpty()).isTrue();
        assertThat(resultat.regelSporing().get("inntektdagsatsperioder")).isEqualTo(
            """
                [{
                    "dagsats": 100.00,
                    "periode": "2025-02-03/2025-02-07"
                }]"""
        );
    }

    @Test
    void skal_redusere_til_0_ved_høyere_sats_for_rapportert_inntekt_enn_sats_delt_på_reduksjonsgrad() {
        // Arrange
        final var fom = LocalDate.of(2025, 2, 3);
        final var tom = fom.plusDays(4);
        final var tidslinjeTilVurdering = new LocalDateTimeline<>(fom, tom, true);
        final var rapportertInntektTidslinje = new LocalDateTimeline<>(fom, tom, Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(501))));
        final var aldersbestemtSatsTidslinje = new LocalDateTimeline<>(fom, tom, BigDecimal.valueOf(66));

        // Act
        final var resultat = new ReduserVedInntektVurderer(
            rapportertInntektTidslinje,
            aldersbestemtSatsTidslinje
        ).vurder(tidslinjeTilVurdering);

        // Assert
        final var forventetResultat = new LocalDateTimeline<>(fom, tom, UttakResultat.forInnvilgelse(BigDecimal.ZERO));
        sammenlignTidslinjer(forventetResultat, resultat.resultatTidslinje());
        assertThat(resultat.restTidslinjeTilVurdering().isEmpty()).isTrue();
        assertThat(resultat.regelSporing().get("inntektdagsatsperioder")).isEqualTo(
            """
                [{
                    "dagsats": 100.20,
                    "periode": "2025-02-03/2025-02-07"
                }]"""
        );
    }

    @Test
    void skal_redusere_ende_på_0_ved_lik_aldersbestemt_sats_og_redusert_inntektssats() {
        // Arrange
        final var fom = LocalDate.of(2025, 2, 3);
        final var tom = fom.plusDays(4);
        final var tidslinjeTilVurdering = new LocalDateTimeline<>(fom, tom, true);
        final var rapportertInntektTidslinje = new LocalDateTimeline<>(fom, tom, Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(500))));
        final var aldersbestemtSatsTidslinje = new LocalDateTimeline<>(fom, tom, BigDecimal.valueOf(66));

        // Act
        final var resultat = new ReduserVedInntektVurderer(
            rapportertInntektTidslinje,
            aldersbestemtSatsTidslinje
        ).vurder(tidslinjeTilVurdering);

        // Assert
        final var forventetResultat = new LocalDateTimeline<>(fom, tom, UttakResultat.forInnvilgelse(BigDecimal.ZERO));
        sammenlignTidslinjer(forventetResultat, resultat.resultatTidslinje());
        assertThat(resultat.restTidslinjeTilVurdering().isEmpty()).isTrue();
        assertThat(resultat.regelSporing().get("inntektdagsatsperioder")).isEqualTo(
            """
                [{
                    "dagsats": 100.00,
                    "periode": "2025-02-03/2025-02-07"
                }]"""
        );
    }


    @Test
    void skal_returnere_restperiode_når_periode_til_vurdering_er_større_enn_rapportert_inntekt_tidslinje() {
        // Arrange
        final var fom = LocalDate.of(2025, 2, 3);
        final var tom = fom.plusDays(4);
        final var tidslinjeTilVurdering = new LocalDateTimeline<>(fom, tom, true);
        final var rapportertInntektTidslinje = new LocalDateTimeline<>(fom, tom.minusDays(1), Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(400))));
        final var aldersbestemtSatsTidslinje = new LocalDateTimeline<>(fom, tom, BigDecimal.valueOf(66));

        // Act
        final var resultat = new ReduserVedInntektVurderer(
            rapportertInntektTidslinje,
            aldersbestemtSatsTidslinje
        ).vurder(tidslinjeTilVurdering);

        // Assert
        final var forventetResultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom.minusDays(1), UttakResultat.forInnvilgelse(BigDecimal.ZERO)),
            new LocalDateSegment<>(tom, tom, UttakResultat.forInnvilgelse(BigDecimal.valueOf(100)))
        ));
        sammenlignTidslinjer(forventetResultat, resultat.resultatTidslinje());
        assertThat(resultat.regelSporing().get("inntektdagsatsperioder")).isEqualTo(
            """
                [{
                    "dagsats": 100.00,
                    "periode": "2025-02-03/2025-02-06"
                }]"""
        );
    }

    private static void sammenlignTidslinjer(LocalDateTimeline<UttakResultat> forventetResultat, LocalDateTimeline<UttakResultat> faktisk) {
        assertThat(faktisk.getLocalDateIntervals()).isEqualTo(forventetResultat.getLocalDateIntervals());
        final var iterator = faktisk.toSegments().iterator();
        while (iterator.hasNext()) {
            final var faktiskSegment = iterator.next();
            final var forventetSegment = forventetResultat.getSegment(faktiskSegment.getLocalDateInterval());
            assertThat(faktiskSegment.getValue().utbetalingsgrad().compareTo(forventetSegment.getValue().utbetalingsgrad())).isEqualTo(0);
            assertThat(faktiskSegment.getValue().avslagsårsak()).isEqualTo(forventetSegment.getValue().avslagsårsak());
        }
    }

}
