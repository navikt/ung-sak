package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class ErEndringIBeregningFRISINNTest {
    private final Beregningsgrunnlag beregningsgrunnlagOrginal = Beregningsgrunnlag.builder()
        .medSkjæringstidspunkt(LocalDate.now())
        .medGrunnbeløp(BigDecimal.valueOf(91425))
        .build();
    private final Beregningsgrunnlag beregningsgrunnlagRevurdering = Beregningsgrunnlag.builder()
        .medSkjæringstidspunkt(LocalDate.now())
        .medGrunnbeløp(BigDecimal.valueOf(91425))
        .build();



    @Test
    public void to_manglende_grunnlag_gir_false() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        boolean resultat = ErEndringIBeregningFRISINN.erUgunst(Optional.empty(), Optional.empty(), new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isFalse();
    }

    @Test
    public void manglende_grunnlag_revuredering_gir_true() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgOrginal(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 300000, List.of(AktivitetStatus.ARBEIDSTAKER));
        boolean resultat = ErEndringIBeregningFRISINN.erUgunst(Optional.empty(),
            Optional.of(beregningsgrunnlagOrginal),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isTrue();
    }

    @Test
    public void manglende_grunnlag_orginalbehandling_gir_false() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgRevurdering(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 300000, List.of(AktivitetStatus.ARBEIDSTAKER));
        boolean resultat = ErEndringIBeregningFRISINN.erUgunst(Optional.of(beregningsgrunnlagRevurdering),
            Optional.empty(),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isFalse();
    }

    @Test
    public void lavere_dagsats_i_orginalbehandliung_gir_false() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgOrginal(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 200000, List.of(AktivitetStatus.ARBEIDSTAKER));
        bgRevurdering(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 250000, List.of(AktivitetStatus.ARBEIDSTAKER));
        boolean resultat = ErEndringIBeregningFRISINN.erUgunst(Optional.of(beregningsgrunnlagRevurdering),
            Optional.of(beregningsgrunnlagOrginal),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isFalse();
    }

    @Test
    public void lavere_dagsats_i_revurdering_gir_true() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgOrginal(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 200000, List.of(AktivitetStatus.ARBEIDSTAKER));
        bgRevurdering(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 150000, List.of(AktivitetStatus.ARBEIDSTAKER));
        boolean resultat = ErEndringIBeregningFRISINN.erUgunst(Optional.of(beregningsgrunnlagRevurdering),
            Optional.of(beregningsgrunnlagOrginal),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isTrue();
    }

    @Test
    public void samme_dagsats_gir_false() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgOrginal(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 200000, List.of(AktivitetStatus.ARBEIDSTAKER));
        bgRevurdering(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 200000, List.of(AktivitetStatus.ARBEIDSTAKER));
        boolean resultat = ErEndringIBeregningFRISINN.erUgunst(Optional.of(beregningsgrunnlagRevurdering),
            Optional.of(beregningsgrunnlagOrginal),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isFalse();
    }

    @Test
    public void ugunst_skal_aggregeres_over_flere_perioder() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30));
        UttakAktivitetPeriode uttakAktivitetPeriode2 = lagUttakPeriode(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));
        int bruttoPrÅr = 200000;
        BigDecimal diffSomIkkeSkaperUgunst = BigDecimal.valueOf(13900);
        BigDecimal nyttBeløp = BigDecimal.valueOf(bruttoPrÅr).subtract(diffSomIkkeSkaperUgunst);

        bgOrginal(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30), bruttoPrÅr, List.of(AktivitetStatus.ARBEIDSTAKER));
        bgOrginal(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31), bruttoPrÅr, List.of(AktivitetStatus.ARBEIDSTAKER));
        bgRevurdering(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30), nyttBeløp.intValue(), List.of(AktivitetStatus.ARBEIDSTAKER));
        bgRevurdering(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31), nyttBeløp.intValue(), List.of(AktivitetStatus.ARBEIDSTAKER));
        boolean resultat = ErEndringIBeregningFRISINN.erUgunst(Optional.of(beregningsgrunnlagRevurdering),
            Optional.of(beregningsgrunnlagOrginal),
            new UttakAktivitet(Arrays.asList(uttakAktivitetPeriode, uttakAktivitetPeriode2)));
        Assertions.assertThat(resultat).isTrue();
    }

    private UttakAktivitetPeriode lagUttakPeriode(LocalDate fom, LocalDate tom) {
        return new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, fom ,tom);
    }

    private void bgRevurdering(LocalDate fom, LocalDate tom, int bruttoPrÅr, List<AktivitetStatus> arbeidsavklaringspenger) {
        lagBGPeriode(fom, tom, bruttoPrÅr, beregningsgrunnlagRevurdering, arbeidsavklaringspenger);
    }

    private void lagBGPeriode(LocalDate fom, LocalDate tom, int bruttoPrÅr, Beregningsgrunnlag beregningsgrunnlag, List<AktivitetStatus> statuser) {
        if (statuser.contains(AktivitetStatus.ARBEIDSTAKER)) {
            BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(fom, tom)
                .medBruttoPrÅr(BigDecimal.valueOf(bruttoPrÅr))
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBruttoPrÅr(BigDecimal.valueOf(bruttoPrÅr))
                    .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(bruttoPrÅr))
                    .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                    .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                        .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))))
                .build(beregningsgrunnlag);

            if (statuser.contains(AktivitetStatus.FRILANSER)) {
                BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.FRILANSER)
                    .medBeregnetPrÅr(BigDecimal.valueOf(bruttoPrÅr))
                    .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(bruttoPrÅr))
                    .build(periode);
            }
            BeregningsgrunnlagPeriode.builder(periode).build(beregningsgrunnlag);
        }
    }

    private void bgOrginal(LocalDate fom, LocalDate tom, int bruttoPrÅr, List<AktivitetStatus> arbeidstaker) {
        lagBGPeriode(fom, tom, bruttoPrÅr, beregningsgrunnlagOrginal, arbeidstaker);
    }


}
