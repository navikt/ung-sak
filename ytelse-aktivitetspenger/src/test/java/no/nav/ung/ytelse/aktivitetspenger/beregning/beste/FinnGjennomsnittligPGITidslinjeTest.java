package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class FinnGjennomsnittligPGITest {

    @Test
    void skal_beregne_gjennomsnittlig_PGI_for_inntekt_under_6G() {
        // Arrange
        var sisteTilgjengeligeGSnittÅr = LocalDate.of(2024, 12, 31);
        var fom = LocalDate.of(2024, 1, 1);
        var tom = LocalDate.of(2024, 1, 31);

        // Inntekt på 500 000 kr (ca 4G) - under 6G
        var inntekt = BigDecimal.valueOf(500000);

        var inntektspost = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(inntekt)
            .medPeriode(fom, tom)
            .build();

        // Act
        LocalDateTimeline<BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost)
        );

        // Assert
        // Når inntekt er under 6G, skal PGI være lik inntekten multiplisert med inflasjonsfaktor
        // Grunnbeløp snitt for 2024 = 124028, inflasjonsfaktor = 124028/124028 = 1.0
        // Forventet PGI = 500000 * 1.0 = 500000
        var segment = resultat.toSegments().first();
        assertThat(segment.getFom()).isEqualTo(fom);
        assertThat(segment.getTom()).isEqualTo(tom);
        assertThat(segment.getValue()).isEqualByComparingTo(BigDecimal.valueOf(500000.0000000000));
    }

    @Test
    void skal_beregne_gjennomsnittlig_PGI_for_inntekt_mellom_6G_og_12G() {
        // Arrange
        var sisteTilgjengeligeGSnittÅr = LocalDate.of(2024, 12, 31);
        var fom = LocalDate.of(2024, 5, 1);
        var tom = LocalDate.of(2024, 5, 31);

        // Inntekt på 9G (mellom 6G og 12G)
        // 9G = 9 * 124028 = 1 116 252 kr
        var niG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(9));

        var inntektspost = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(niG)
            .medPeriode(fom, tom)
            .build();

        // Act
        LocalDateTimeline<BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost)
        );

        // Assert
        // For inntekt mellom 6G og 12G skal PGI være: 6G + ((inntekt - 6G) / 3)
        // 6G = 6 * 124028 = 744 168
        // PGI = 744168 + ((1116252 - 744168) / 3) = 744168 + 124028 = 868 196
        // Med inflasjonsfaktor 1.0: 868196 * 1.0 = 868196
        var segment = resultat.toSegments().first();
        assertThat(segment.getFom()).isEqualTo(fom);
        assertThat(segment.getTom()).isEqualTo(tom);
        assertThat(segment.getValue()).isEqualByComparingTo(BigDecimal.valueOf(868196.0000000000));
    }

    @Test
    void skal_beregne_gjennomsnittlig_PGI_for_inntekt_over_12G() {
        // Arrange
        var sisteTilgjengeligeGSnittÅr = LocalDate.of(2024, 12, 31);
        var fom = LocalDate.of(2024, 5, 1);
        var tom = LocalDate.of(2024, 5, 31);

        // Inntekt på 15G (over 12G) = 15 * 124028 = 1 860 420 kr
        var femtenG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(15));

        var inntektspost = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(femtenG)
            .medPeriode(fom, tom)
            .build();

        // Act
        LocalDateTimeline<BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost)
        );

        // Assert
        // For inntekt over 12G skal PGI maks være: 6G + ((12G - 6G) / 3) = 6G + 2G = 8G
        // 8G = 8 * 124028 = 992 224
        // Med inflasjonsfaktor 1.0: 992224 * 1.0 = 992224
        var segment = resultat.toSegments().first();
        assertThat(segment.getFom()).isEqualTo(fom);
        assertThat(segment.getTom()).isEqualTo(tom);
        assertThat(segment.getValue()).isEqualByComparingTo(BigDecimal.valueOf(992224.0000000000));
    }

    @Test
    void skal_filtrere_ut_ytelser_fra_inntekter() {
        // Arrange
        var sisteTilgjengeligeGSnittÅr = LocalDate.of(2024, 12, 31);
        var fom = LocalDate.of(2024, 5, 1);
        var tom = LocalDate.of(2024, 5, 31);

        var lønnsinntekt = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(BigDecimal.valueOf(500000))
            .medPeriode(fom, tom)
            .build();

        var ytelsesinntekt = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.YTELSE)
            .medBeløp(BigDecimal.valueOf(100000))
            .medPeriode(fom, tom)
            .build();

        // Act
        LocalDateTimeline<BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(lønnsinntekt, ytelsesinntekt)
        );

        // Assert
        // Ytelser skal filtreres ut, så vi skal kun ha bidrag fra lønnsinntekt på 500 000
        // Med inflasjonsfaktor 1.0: 500000 * 1.0 = 500000
        var segment = resultat.toSegments().first();
        assertThat(segment.getFom()).isEqualTo(fom);
        assertThat(segment.getTom()).isEqualTo(tom);
        assertThat(segment.getValue()).isEqualByComparingTo(BigDecimal.valueOf(500000.0000000000));
    }

    @Test
    void skal_håndtere_flere_inntektsperioder() {
        // Arrange
        var sisteTilgjengeligeGSnittÅr = LocalDate.of(2024, 12, 31);

        var inntektspost1 = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(BigDecimal.valueOf(300000))
            .medPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
            .build();

        var inntektspost2 = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(BigDecimal.valueOf(400000))
            .medPeriode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 31))
            .build();

        // Act
        LocalDateTimeline<BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost1, inntektspost2)
        );

        // Assert
        // Første periode: 300000 * 1.0 = 300000
        var segmenter = resultat.toSegments();
        assertThat(segmenter.size()).isEqualTo(2);

        var første = segmenter.first();
        assertThat(første.getFom()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(første.getTom()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(første.getValue()).isEqualByComparingTo(BigDecimal.valueOf(300000.0000000000));

        // Andre periode: 400000 * 1.0 = 400000
        var andre = segmenter.last();
        assertThat(andre.getFom()).isEqualTo(LocalDate.of(2024, 7, 1));
        assertThat(andre.getTom()).isEqualTo(LocalDate.of(2024, 7, 31));
        assertThat(andre.getValue()).isEqualByComparingTo(BigDecimal.valueOf(400000.0000000000));
    }

    @Test
    void skal_beregne_med_inflasjonsfaktor_for_tidligere_år() {
        // Arrange
        var sisteTilgjengeligeGSnittÅr = LocalDate.of(2024, 12, 31);
        var fom = LocalDate.of(2023, 5, 1);
        var tom = LocalDate.of(2023, 5, 31);

        var inntektspost = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(BigDecimal.valueOf(500000))
            .medPeriode(fom, tom)
            .build();

        // Act
        LocalDateTimeline<BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost)
        );

        // Assert
        // Grunnbeløp snitt 2023 = 118620, Grunnbeløp snitt 2024 = 124028
        // Inflasjonsfaktor = 124028 / 118620 = 1.0456
        // Forventet PGI = 500000 * 1.0456 ≈ 522800
        var segment = resultat.toSegments().first();
        assertThat(segment.getFom()).isEqualTo(fom);
        assertThat(segment.getTom()).isEqualTo(tom);

        // Sjekk at inflasjonsjustering har funnet sted
        var forventetInflasjonsfaktor = BigDecimal.valueOf(124028).divide(BigDecimal.valueOf(118620), 10, RoundingMode.HALF_EVEN);
        var forventetPGI = BigDecimal.valueOf(500000).multiply(forventetInflasjonsfaktor);
        assertThat(segment.getValue()).isEqualByComparingTo(forventetPGI);
    }

    @Test
    void skal_beregne_korrekt_PGI_bidrag_nøyaktig_på_6G() {
        // Arrange
        var sisteTilgjengeligeGSnittÅr = LocalDate.of(2024, 12, 31);
        var fom = LocalDate.of(2024, 5, 1);
        var tom = LocalDate.of(2024, 5, 31);

        // Nøyaktig 6G = 6 * 124028 = 744168
        var seksG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(6));

        var inntektspost = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(seksG)
            .medPeriode(fom, tom)
            .build();

        // Act
        LocalDateTimeline<BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost)
        );

        // Assert
        // Ved nøyaktig 6G skal PGI være lik 6G (ingen reduksjon)
        // Med inflasjonsfaktor 1.0: 744168 * 1.0 = 744168
        var segment = resultat.toSegments().first();
        assertThat(segment.getFom()).isEqualTo(fom);
        assertThat(segment.getTom()).isEqualTo(tom);
        assertThat(segment.getValue()).isEqualByComparingTo(BigDecimal.valueOf(744168.0000000000));
    }

    @Test
    void skal_beregne_korrekt_PGI_bidrag_nøyaktig_på_12G() {
        // Arrange
        var sisteTilgjengeligeGSnittÅr = LocalDate.of(2024, 12, 31);
        var fom = LocalDate.of(2024, 5, 1);
        var tom = LocalDate.of(2024, 5, 31);

        // Nøyaktig 12G = 12 * 124028 = 1 488 336
        var tolvG = BigDecimal.valueOf(124028).multiply(BigDecimal.valueOf(12));

        var inntektspost = InntektspostBuilder.ny()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(tolvG)
            .medPeriode(fom, tom)
            .build();

        // Act
        LocalDateTimeline<BigDecimal> resultat = FinnGjennomsnittligPGI.finnGjennomsnittligPGI(
            sisteTilgjengeligeGSnittÅr,
            List.of(inntektspost)
        );

        // Assert
        // Ved 12G skal PGI-bidrag være 8G: 6G + ((12G - 6G) / 3) = 6G + 2G = 8G
        // 8G = 8 * 124028 = 992 224
        // Med inflasjonsfaktor 1.0: 992224 * 1.0 = 992224
        var segment = resultat.toSegments().first();
        assertThat(segment.getFom()).isEqualTo(fom);
        assertThat(segment.getTom()).isEqualTo(tom);
        assertThat(segment.getValue()).isEqualByComparingTo(BigDecimal.valueOf(992224.0000000000));
    }
}

