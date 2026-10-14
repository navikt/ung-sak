package no.nav.ung.sak.kontroll;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InntektMedNedreGrenseTest {

    @Test
    void skal_gi_null_for_negativ_inntekt() {
        // Arrange
        final var inntekter = Set.of(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(-5830.54)));

        // Act
        final var sum = InntektMedNedreGrense.summerForType(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, inntekter);

        // Assert
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void skal_ikke_motregne_negativ_inntekt_mot_annen_inntektstype() {
        // Arrange
        final var inntekter = Set.of(
            new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(-4000)),
            new RapportertInntekt(InntektType.YTELSE, BigDecimal.valueOf(10000)));

        // Act
        final var sum = InntektMedNedreGrense.summerAlleTyper(inntekter);

        // Assert
        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    void skal_summere_positive_inntekter_for_alle_typer() {
        // Arrange
        final var inntekter = Set.of(
            new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, BigDecimal.valueOf(4000)),
            new RapportertInntekt(InntektType.YTELSE, BigDecimal.valueOf(10000)));

        // Act
        final var sum = InntektMedNedreGrense.summerAlleTyper(inntekter);

        // Assert
        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(14000));
    }

    @Test
    void skal_gi_null_uten_inntekter() {
        // Act
        final var sum = InntektMedNedreGrense.summerAlleTyper(Set.of());

        // Assert
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
