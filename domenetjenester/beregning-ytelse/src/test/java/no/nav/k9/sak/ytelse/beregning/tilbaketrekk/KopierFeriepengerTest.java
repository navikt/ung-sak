package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.Test;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class KopierFeriepengerTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 20);
    private static final LocalDate SLUTTDATO = LocalDate.of(2019, Month.APRIL, 13);
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("900050001");

    @Test
    public void kopierUtenFeriepenger() {
        // Arrange
        BeregningsresultatPeriode bgBrPeriode = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SLUTTDATO);
        lagAndel(bgBrPeriode, false, 1000);

        BeregningsresultatEntitet utbetTY = lagBeregningsresultat();
        BeregningsresultatPeriode utbetP0 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, LocalDate.of(2019, Month.JANUARY, 31))
            .build(utbetTY);
        lagAndel(utbetP0, true, 1000);
        BeregningsresultatPeriode utbetP1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2019, Month.FEBRUARY, 1), SLUTTDATO)
            .build(utbetTY);
        lagAndel(utbetP1, true, 0);
        lagAndel(utbetP1, false, 1000);

        // Act
        KopierFeriepenger.kopierFraTil(1L, bgBrPeriode.getBeregningsresultat(), utbetTY);

        // Assert
        assertThat(utbetTY.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty();
    }

    private BeregningsresultatEntitet lagBeregningsresultat() {
        return BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
    }

    @Test
    public void kopierMedFeriepenger() {
        // Arrange
        BeregningsresultatPeriode bgBrPeriode = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SLUTTDATO);
        BeregningsresultatEntitet bgBeregningsresultatFP = bgBrPeriode.getBeregningsresultat();
        var andel = lagAndel(bgBrPeriode, false, 1000);
        BeregningsresultatFeriepengerPrÅr.builder().medOpptjeningsår(SKJÆRINGSTIDSPUNKT)
            .medÅrsbeløp(200L)
        .buildFor(andel);

        BeregningsresultatEntitet utbetTY = lagBeregningsresultat();
        BeregningsresultatPeriode utbetP0 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, LocalDate.of(2019, Month.JANUARY, 31))
            .build(utbetTY);
        lagAndel(utbetP0, true, 1000);
        BeregningsresultatPeriode utbetP1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2019, Month.FEBRUARY, 1), SLUTTDATO)
            .build(utbetTY);
        lagAndel(utbetP1, true, 0);
        lagAndel(utbetP1, false, 1000);

        // Act
        KopierFeriepenger.kopierFraTil(1L, bgBeregningsresultatFP, utbetTY);

        // Assert
        List<BeregningsresultatPeriode> beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);
        List<BeregningsresultatAndel> utbetP0Andeler = beregningsresultatPerioder.get(0).getBeregningsresultatAndelList();
        assertThat(utbetP0Andeler).hasSize(2);
        assertThat(utbetP0Andeler.get(0).getBeregningsresultatFeriepengerPrÅrListe()).isEmpty();
        BeregningsresultatAndel utbetP0AndelArbeidsgiver = utbetP0Andeler.get(1);
        assertThat(utbetP0AndelArbeidsgiver.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1);
        assertThat(utbetTY.getBeregningsresultatFeriepengerPrÅrListe()).isNotEmpty();
        assertThat(utbetTY.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1);
        assertThat(utbetTY.getBeregningsresultatFeriepengerPrÅrListe().get(0)).satisfies(prÅr ->
            assertThat(prÅr.getBeregningsresultatAndel()).isSameAs(utbetP0AndelArbeidsgiver)
        );
    }

    private BeregningsresultatPeriode lagBeregningsresultatPeriode(LocalDate fom, LocalDate tom) {
        BeregningsresultatEntitet br = lagBeregningsresultat();
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(br);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode brPeriode, boolean erBrukerMottaker, int dagsats) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(erBrukerMottaker)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsatsFraBg(dagsats)
            .medDagsats(dagsats)
            .medArbeidsgiver(ARBEIDSGIVER)
            .buildFor(brPeriode);
    }
}
