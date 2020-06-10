package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

public class ErEndringIBeregningFRISINNTest {
    private Beregningsgrunnlag beregningsgrunnlagOrginal = Beregningsgrunnlag.builder()
        .medSkjæringstidspunkt(LocalDate.now())
        .medGrunnbeløp(BigDecimal.valueOf(91425))
        .build();
    private Beregningsgrunnlag beregningsgrunnlagRevurdering = Beregningsgrunnlag.builder()
        .medSkjæringstidspunkt(LocalDate.now())
        .medGrunnbeløp(BigDecimal.valueOf(91425))
        .build();

    @Test
    public void to_manglende_grunnlag_gir_false() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        boolean resultat = ErEndringIBeregningFRISINN.vurder(Optional.empty(), Optional.empty(), new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isFalse();
    }

    @Test
    public void manglende_grunnlag_revuredering_gir_true() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgOrginal(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 300000);
        boolean resultat = ErEndringIBeregningFRISINN.vurder(Optional.empty(),
            Optional.of(beregningsgrunnlagOrginal),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isTrue();
    }

    @Test
    public void manglende_grunnlag_orginalbehandling_gir_false() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgRevurdering(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 300000);
        boolean resultat = ErEndringIBeregningFRISINN.vurder(Optional.of(beregningsgrunnlagRevurdering),
            Optional.empty(),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isFalse();
    }

    @Test
    public void lavere_dagsats_i_orginalbehandliung_gir_false() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgOrginal(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 200000);
        bgRevurdering(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 250000);
        boolean resultat = ErEndringIBeregningFRISINN.vurder(Optional.of(beregningsgrunnlagRevurdering),
            Optional.of(beregningsgrunnlagOrginal),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isFalse();
    }

    @Test
    public void lavere_dagsats_i_revurdering_gir_true() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgOrginal(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 200000);
        bgRevurdering(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 150000);
        boolean resultat = ErEndringIBeregningFRISINN.vurder(Optional.of(beregningsgrunnlagRevurdering),
            Optional.of(beregningsgrunnlagOrginal),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isTrue();
    }

    @Test
    public void samme_dagsats_gir_false() {
        UttakAktivitetPeriode uttakAktivitetPeriode = lagUttakPeriode(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
        bgOrginal(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 200000);
        bgRevurdering(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30), 200000);
        boolean resultat = ErEndringIBeregningFRISINN.vurder(Optional.of(beregningsgrunnlagRevurdering),
            Optional.of(beregningsgrunnlagOrginal),
            new UttakAktivitet(Collections.singletonList(uttakAktivitetPeriode)));
        Assertions.assertThat(resultat).isFalse();
    }

    private UttakAktivitetPeriode lagUttakPeriode(LocalDate fom, LocalDate tom) {
        return new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, fom ,tom);
    }

    private void bgRevurdering(LocalDate fom, LocalDate tom, int bruttoPrÅr) {
        lagBGPeriode(fom, tom, bruttoPrÅr, beregningsgrunnlagRevurdering);
    }

    private void bgOrginal(LocalDate fom, LocalDate tom, int bruttoPrÅr) {
        lagBGPeriode(fom, tom, bruttoPrÅr, beregningsgrunnlagOrginal);
    }


    private void lagBGPeriode(LocalDate fom, LocalDate tom, int bruttoPrÅr, Beregningsgrunnlag beregningsgrunnlag) {
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(fom, tom)
            .medBruttoPrÅr(BigDecimal.valueOf(bruttoPrÅr))
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBruttoPrÅr(BigDecimal.valueOf(bruttoPrÅr))
                .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(bruttoPrÅr))
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                    .medArbeidsgiver(Arbeidsgiver.virksomhet("000000000"))))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medBeregnetPrÅr(BigDecimal.valueOf(bruttoPrÅr))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(bruttoPrÅr))
            .build(periode);
        BeregningsgrunnlagPeriode.builder(periode).build(beregningsgrunnlag);
    }

}
