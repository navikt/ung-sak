package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.InntektType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LagTilkjentYtelseTest {

    public static final LocalDateTimeline<RapporterteInntekter> TOM_TIDSLINJE = LocalDateTimeline.empty();

    @Test
    void testLagTidslinjeMedTomGodkjentTidslinje() {
        // Setup test data
        LocalDateTimeline<Boolean> godkjentTidslinje = new LocalDateTimeline<>(List.of());

        LocalDateTimeline<BeregnetSats> totalsatsTidslinje = new LocalDateTimeline<>(List.of(
            lagSatsperiode(BigDecimal.valueOf(100), 200, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31)),
            lagSatsperiode(BigDecimal.valueOf(150), 250, LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28))
        ));

        LocalDateTimeline<RapporterteInntekter> rapportertInntektTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2023, 1, 15), LocalDate.of(2023, 2, 15),
                new RapporterteInntekter(Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(50000)))))
        ));

        // Call the method under test
        LocalDateTimeline<TilkjentYtelseVerdi> resultat = LagTilkjentYtelse.lagTidslinje(godkjentTidslinje, totalsatsTidslinje, rapportertInntektTidslinje);

        // Verify the result
        assertNotNull(resultat);
        assertTrue(resultat.isEmpty());
    }

    @Test
    void testLagTidslinjeUtenRapportertInntekt() {
        // Arrange
        final var fom1 = LocalDate.of(2023, 1, 1);
        final var tom1 = LocalDate.of(2023, 1, 31);
        final var fom2 = LocalDate.of(2023, 2, 1);
        final var tom2 = LocalDate.of(2023, 2, 28);

        LocalDateTimeline<Boolean> godkjentTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom1, tom1, true),
            new LocalDateSegment<>(fom2, tom2, true)
        ));

        final var grunnsats1 = BigDecimal.valueOf(100);
        final var grunnsats2 = BigDecimal.valueOf(150);
        final var barnetilleggSats1 = 200;
        final var barnetilleggSats2 = 250;
        LocalDateTimeline<BeregnetSats> totalsatsTidslinje = new LocalDateTimeline<>(List.of(
            lagSatsperiode(grunnsats1, barnetilleggSats1, fom1, tom1),
            lagSatsperiode(grunnsats2, barnetilleggSats2, fom2, tom2)
        ));

        LocalDateTimeline<RapporterteInntekter> rapportertInntektTidslinje = TOM_TIDSLINJE;

        // Act
        LocalDateTimeline<TilkjentYtelseVerdi> resultat = LagTilkjentYtelse.lagTidslinje(godkjentTidslinje, totalsatsTidslinje, rapportertInntektTidslinje);

        // Assert
        // Forventer ingen reduksjon
        assertNotNull(resultat);
        assertEquals(2, resultat.getLocalDateIntervals().size());

        final var iterator = resultat.toSegments().iterator();
        final var forventetDagsats1 = BigDecimal.valueOf(14);
        final var forventetUredusertBeløp1 = grunnsats1.add(BigDecimal.valueOf(barnetilleggSats1));
        LocalDateSegment<TilkjentYtelseVerdi> segment1 = iterator.next();
        assertSegment(segment1, fom1, tom1, forventetUredusertBeløp1, forventetDagsats1, BigDecimal.ZERO, forventetUredusertBeløp1, 100);

        LocalDateSegment<TilkjentYtelseVerdi> segment2 = iterator.next();
        final var forventetUredusertBeløp2 = grunnsats2.add(BigDecimal.valueOf(barnetilleggSats2));
        final var forventetDagsats2 = BigDecimal.valueOf(20);
        assertSegment(segment2, fom2, tom2, forventetUredusertBeløp2, forventetDagsats2, BigDecimal.ZERO, forventetUredusertBeløp2, 100);
    }

    @Test
    void skalLageTilkjentYtelseForRapportertInntektMindreEnnGrunnsatsOgStørreEnnBarnetillegg() {
        // Arrange
        final var fom = LocalDate.of(2023, 1, 1);
        final var tom = LocalDate.of(2023, 1, 31);

        LocalDateTimeline<Boolean> godkjentTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, true)
        ));

        final var grunnsats = BigDecimal.valueOf(200);
        final var barnetilleggSats = 50;
        LocalDateTimeline<BeregnetSats> totalsatsTidslinje = new LocalDateTimeline<>(List.of(
            lagSatsperiode(grunnsats, barnetilleggSats, fom, tom)
        ));

        final var rapportertInntekt = BigDecimal.valueOf(150);
        LocalDateTimeline<RapporterteInntekter> rapportertInntektTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, new RapporterteInntekter(Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, rapportertInntekt))))
        ));

        // Act
        LocalDateTimeline<TilkjentYtelseVerdi> resultat = LagTilkjentYtelse.lagTidslinje(godkjentTidslinje, totalsatsTidslinje, rapportertInntektTidslinje);

        // Assert
        assertNotNull(resultat);
        assertEquals(1, resultat.getLocalDateIntervals().size());

        final var iterator = resultat.toSegments().iterator();
        final var forventetDagsats = BigDecimal.valueOf(7);
        final var forventetReduksjon = rapportertInntekt.multiply(BigDecimal.valueOf(0.66));
        final var forventetUredusertBeløp = grunnsats.add(BigDecimal.valueOf(barnetilleggSats));
        final var forventetRedusertBeløp = forventetUredusertBeløp.subtract(forventetReduksjon);
        LocalDateSegment<TilkjentYtelseVerdi> segment = iterator.next();
        assertSegment(segment,
            fom,
            tom,
            forventetUredusertBeløp,
            forventetDagsats,
            forventetReduksjon,
            forventetRedusertBeløp,
            76);
    }



    private static void assertSegment(LocalDateSegment<TilkjentYtelseVerdi> segment2,
                                      LocalDate fom,
                                      LocalDate tom,
                                      BigDecimal forventetUredusertBeløp,
                                      BigDecimal forventetDagsats,
                                      BigDecimal reduksjon,
                                      BigDecimal forventetRedusertBeløp, int utbetalingsgrad) {
        assertEquals(fom, segment2.getFom());
        assertEquals(tom, segment2.getTom());
        assertThat(segment2.getValue().uredusertBeløp()).isEqualByComparingTo(forventetUredusertBeløp);
        assertThat(segment2.getValue().reduksjon()).isEqualByComparingTo(reduksjon);
        assertThat(segment2.getValue().redusertBeløp()).isEqualByComparingTo(forventetRedusertBeløp);
        assertThat(segment2.getValue().dagsats()).isEqualByComparingTo(forventetDagsats);
        assertEquals(utbetalingsgrad, segment2.getValue().utbetalingsgrad());
    }

    private static LocalDateSegment<BeregnetSats> lagSatsperiode(BigDecimal grunnsats1, int barnetilleggSats1, LocalDate fom, LocalDate tom) {
        return new LocalDateSegment<>(fom, tom, new BeregnetSats(grunnsats1, barnetilleggSats1));
    }


}
