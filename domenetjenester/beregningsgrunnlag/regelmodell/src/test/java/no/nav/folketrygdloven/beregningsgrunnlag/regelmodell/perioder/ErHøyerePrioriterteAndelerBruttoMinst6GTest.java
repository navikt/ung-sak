package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.perioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.perioder.ErHøyerePrioriterteAndelerBruttoMinst6G;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatusV2;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AndelGraderingImpl;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.BruttoBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodisertBruttoBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.vedtak.konfig.Tid;

public class ErHøyerePrioriterteAndelerBruttoMinst6GTest {
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90_000);
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.FEBRUARY, 14);

    private static final BigDecimal MÅNEDSBELØP_UNDER_6G = BigDecimal.valueOf(40_000);
    private static final BigDecimal MÅNEDSBELØP_PÅ_6G = BigDecimal.valueOf(45_000);
    private static final BigDecimal MÅNEDSBELØP_OVER_6G = BigDecimal.valueOf(70_000);

    @Test
    public void gradert_SN_selvstendigErLaverePrioritertEnnFL() {
        // Arrange
        PeriodisertBruttoBeregningsgrunnlag periodisertBg = PeriodisertBruttoBeregningsgrunnlag.builder()
            .medPeriode(Periode.of(SKJÆRINGSTIDSPUNKT, Tid.TIDENES_ENDE))
            .leggTilBruttoBeregningsgrunnlag(BruttoBeregningsgrunnlag.builder()
                .medAktivitetStatus(AktivitetStatusV2.SN)
                .medBruttoBeregningsgrunnlag(MÅNEDSBELØP_UNDER_6G.multiply(BigDecimal.valueOf(12)))
                .build())
            .leggTilBruttoBeregningsgrunnlag(BruttoBeregningsgrunnlag.builder()
                .medAktivitetStatus(AktivitetStatusV2.FL)
                .medBruttoBeregningsgrunnlag(MÅNEDSBELØP_OVER_6G.multiply(BigDecimal.valueOf(12)))
                .build())
            .build();

        var gradering = new Gradering(new Periode(LocalDate.of(2019, Month.MARCH, 1), Tid.TIDENES_ENDE), BigDecimal.valueOf(50));
        AndelGraderingImpl andelGradering = AndelGraderingImpl.builder()
            .medAktivitetStatus(AktivitetStatusV2.SN)
            .medGraderinger(List.of(gradering))
            .build();

        // Act
        boolean resultat = ErHøyerePrioriterteAndelerBruttoMinst6G.vurder(GRUNNBELØP, periodisertBg, andelGradering);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void gradert_frilans_frilansErHøyerePrioritertEnnSN() {
        // Arrange
        PeriodisertBruttoBeregningsgrunnlag periodisertBg = PeriodisertBruttoBeregningsgrunnlag.builder()
            .medPeriode(Periode.of(SKJÆRINGSTIDSPUNKT, Tid.TIDENES_ENDE))
            .leggTilBruttoBeregningsgrunnlag(BruttoBeregningsgrunnlag.builder()
                .medAktivitetStatus(AktivitetStatusV2.SN)
                .medBruttoBeregningsgrunnlag(MÅNEDSBELØP_UNDER_6G.multiply(BigDecimal.valueOf(12)))
                .build())
            .leggTilBruttoBeregningsgrunnlag(BruttoBeregningsgrunnlag.builder()
                .medAktivitetStatus(AktivitetStatusV2.FL)
                .medBruttoBeregningsgrunnlag(MÅNEDSBELØP_PÅ_6G.multiply(BigDecimal.valueOf(12)))
                .build())
            .build();

        var gradering = new Gradering(new Periode(LocalDate.of(2019, Month.MARCH, 1), Tid.TIDENES_ENDE), BigDecimal.valueOf(50));
        AndelGraderingImpl andelGradering = AndelGraderingImpl.builder()
            .medAktivitetStatus(AktivitetStatusV2.FL)
            .medGraderinger(List.of(gradering))
            .build();

        // Act
        boolean resultat = ErHøyerePrioriterteAndelerBruttoMinst6G.vurder(GRUNNBELØP, periodisertBg, andelGradering);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void gradert_frilans_frilansErLaverePrioritertEnnAT() {
        // Arrange
        PeriodisertBruttoBeregningsgrunnlag periodisertBg = PeriodisertBruttoBeregningsgrunnlag.builder()
            .medPeriode(Periode.of(SKJÆRINGSTIDSPUNKT, Tid.TIDENES_ENDE))
            .leggTilBruttoBeregningsgrunnlag(BruttoBeregningsgrunnlag.builder()
                .medAktivitetStatus(AktivitetStatusV2.AT)
                .medArbeidsforhold(Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("orgnr"))
                .medBruttoBeregningsgrunnlag(MÅNEDSBELØP_PÅ_6G.multiply(BigDecimal.valueOf(12)))
                .build())
            .leggTilBruttoBeregningsgrunnlag(BruttoBeregningsgrunnlag.builder()
                .medAktivitetStatus(AktivitetStatusV2.FL)
                .medBruttoBeregningsgrunnlag(MÅNEDSBELØP_UNDER_6G.multiply(BigDecimal.valueOf(12)))
                .build())
            .build();

        var gradering = new Gradering(new Periode(LocalDate.of(2019, Month.MARCH, 1), Tid.TIDENES_ENDE), BigDecimal.valueOf(50));
        AndelGraderingImpl andelGradering = AndelGraderingImpl.builder()
            .medAktivitetStatus(AktivitetStatusV2.FL)
            .medGraderinger(List.of(gradering))
            .build();

        // Act
        boolean resultat = ErHøyerePrioriterteAndelerBruttoMinst6G.vurder(GRUNNBELØP, periodisertBg, andelGradering);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void gradert_frilans_BGUnder6G() {
        // Arrange
        PeriodisertBruttoBeregningsgrunnlag periodisertBg = PeriodisertBruttoBeregningsgrunnlag.builder()
            .medPeriode(Periode.of(SKJÆRINGSTIDSPUNKT, Tid.TIDENES_ENDE))
            .leggTilBruttoBeregningsgrunnlag(BruttoBeregningsgrunnlag.builder()
                .medAktivitetStatus(AktivitetStatusV2.AT)
                .medArbeidsforhold(Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("orgnr"))
                .medBruttoBeregningsgrunnlag(MÅNEDSBELØP_UNDER_6G.multiply(BigDecimal.valueOf(12)))
                .build())
            .leggTilBruttoBeregningsgrunnlag(BruttoBeregningsgrunnlag.builder()
                .medAktivitetStatus(AktivitetStatusV2.FL)
                .medBruttoBeregningsgrunnlag(MÅNEDSBELØP_UNDER_6G.multiply(BigDecimal.valueOf(12)))
                .build())
            .build();

        var gradering = new Gradering(new Periode(LocalDate.of(2019, Month.MARCH, 1), Tid.TIDENES_ENDE), BigDecimal.valueOf(50));
        AndelGraderingImpl andelGradering = AndelGraderingImpl.builder()
            .medAktivitetStatus(AktivitetStatusV2.FL)
            .medGraderinger(List.of(gradering))
            .build();

        // Act
        boolean resultat = ErHøyerePrioriterteAndelerBruttoMinst6G.vurder(GRUNNBELØP, periodisertBg, andelGradering);

        // Assert
        assertThat(resultat).isFalse();
    }
}
