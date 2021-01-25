package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;

public class KopierFeriepengerTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 20);
    private static final LocalDate SLUTTDATO = LocalDate.of(2019, Month.APRIL, 13);
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("900050001");

    @Test
    public void kopierUtenFeriepenger() {
        // Arrange
        BeregningsresultatPeriode bgBrPeriode = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SLUTTDATO);
        lagAndel(bgBrPeriode, false, 1000, 0);

        BeregningsresultatEntitet utbetTY = lagBeregningsresultat();
        BeregningsresultatPeriode utbetP0 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, LocalDate.of(2019, Month.JANUARY, 31))
            .build(utbetTY);
        lagAndel(utbetP0, true, 1000, 0);
        BeregningsresultatPeriode utbetP1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2019, Month.FEBRUARY, 1), SLUTTDATO)
            .build(utbetTY);
        lagAndel(utbetP1, true, 0, 0);
        lagAndel(utbetP1, false, 1000, 0);

        // Act
        KopierFeriepenger.kopierFraTil(1L, bgBrPeriode.getBeregningsresultat(), utbetTY);

        // Assert
        assertThat(utbetTY.getBeregningsresultatAndelTimeline().toSegments()).allSatisfy(seg -> {
            assertThat(seg.getValue()).hasSize(2);
            assertThat(seg.getValue().get(0).getFeriepengerÅrsbeløp()).isNull();
        });
    }

    private BeregningsresultatEntitet lagBeregningsresultat() {
        return BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
    }

    @SuppressWarnings("unused")
    @Test
    public void kopierMedFeriepenger() {
        // Arrange
        var bgBrPeriode = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SLUTTDATO);
        var bgBeregningsresultat = bgBrPeriode.getBeregningsresultat();
        var andel = lagAndel(bgBrPeriode, false, 1000, 200);

        var utbetTY = lagBeregningsresultat();
        var utbetP0 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, LocalDate.of(2019, Month.JANUARY, 31))
            .build(utbetTY);
        lagAndel(utbetP0, true, 1000, 0);
        var utbetP1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2019, Month.FEBRUARY, 1), SLUTTDATO)
            .build(utbetTY);
        lagAndel(utbetP1, true, 0, 0);
        lagAndel(utbetP1, false, 1000, 0);

        // Act
        KopierFeriepenger.kopierFraTil(1L, bgBeregningsresultat, utbetTY);

        // Assert
        var beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);

        var segments = utbetTY.getBeregningsresultatAndelTimeline().toSegments();
        var år1 = segments.first();
        var år2 = segments.last();
        assertThat(år1).isNotSameAs(år2);

        assertThat(år1).satisfies(seg -> {
            assertThat(seg.getValue()).anyMatch(a -> !a.erBrukerMottaker() && new Beløp(200).equals(a.getFeriepengerÅrsbeløp()));
            assertThat(seg.getValue()).anyMatch(a -> a.erBrukerMottaker() && a.getFeriepengerÅrsbeløp() == null);
        });


        assertThat(år1.getValue()).anySatisfy(a -> {
            assertThat(a.getFeriepengerÅrsbeløp()).isNotNull().satisfies(c -> equals(c.getVerdi(), 200));
            assertThat(a.erBrukerMottaker()).isFalse();
        });

        assertThat(år2.getValue()).allSatisfy(a -> {
            assertThat(a.getFeriepengerÅrsbeløp()).isNull();
        });
    }

    private static boolean equals(BigDecimal actual, long expected) {
        return actual.compareTo(BigDecimal.valueOf(expected)) == 0;
    }

    private BeregningsresultatPeriode lagBeregningsresultatPeriode(LocalDate fom, LocalDate tom) {
        var br = lagBeregningsresultat();
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(br);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode brPeriode, boolean erBrukerMottaker, int dagsats, int feriepenger) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(erBrukerMottaker)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(dagsats)
            .medDagsats(dagsats)
            .medArbeidsgiver(ARBEIDSGIVER)
            .medFeriepengerÅrsbeløp(feriepenger > 0 ? new Beløp(feriepenger) : null)
            .buildFor(brPeriode);
    }
}
