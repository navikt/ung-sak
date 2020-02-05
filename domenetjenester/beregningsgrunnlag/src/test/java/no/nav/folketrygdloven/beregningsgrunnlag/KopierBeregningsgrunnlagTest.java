package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.KopierBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.typer.AktørId;

public class KopierBeregningsgrunnlagTest {

    private static final BigDecimal OVERSTYRT_PR_ÅR = BigDecimal.valueOf(7331);
    private static final BigDecimal BEREGNET_PR_ÅR = BigDecimal.valueOf(1337);

    @Test
    public void kopierOgOpprettAndelerVedKunYtelse() {
        // Arrange

        BeregningsgrunnlagEntitet gammeltBeregningsgrunnlag = lagBeregningsgrunnlagKunYtelse(true, false);
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = lagBeregningsgrunnlagKunYtelse(false, false);

        // Act
        KopierBeregningsgrunnlag.kopierVerdier(gammeltBeregningsgrunnlag, nyttBeregningsgrunnlag);

        BeregningsgrunnlagPeriode nyBgPeriode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        // Assert
        assertThat(nyBgPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel = nyBgPeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream().filter(andel -> andel.getAndelsnr().equals(1L)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel nyAndel = nyBgPeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream().filter(andel -> andel.getAndelsnr().equals(3L)).findFirst().get();

        assertThat(eksisterendeAndel.getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
        assertThat(eksisterendeAndel.getBeregnetPrÅr()).isEqualByComparingTo(BEREGNET_PR_ÅR);
        assertThat(eksisterendeAndel.getLagtTilAvSaksbehandler()).isFalse();
        assertThat(eksisterendeAndel.getAktivitetStatus()).isEqualByComparingTo(AktivitetStatus.BRUKERS_ANDEL);

        assertThat(nyAndel.getInntektskategori()).isEqualTo(Inntektskategori.SJØMANN);
        assertThat(nyAndel.getBeregnetPrÅr()).isEqualByComparingTo(BEREGNET_PR_ÅR);
        assertThat(nyAndel.getLagtTilAvSaksbehandler()).isTrue();
        assertThat(nyAndel.getAktivitetStatus()).isEqualByComparingTo(AktivitetStatus.BRUKERS_ANDEL);
    }


    @Test
    public void kopierAndelerVedKombinasjonMedArbeidstakerForKunYtelse() {
        // Arrange

        BeregningsgrunnlagEntitet gammeltBeregningsgrunnlag = lagBeregningsgrunnlagKunYtelse(true, true);
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = lagBeregningsgrunnlagKunYtelse(false, true);

        // Act
        KopierBeregningsgrunnlag.kopierVerdier(gammeltBeregningsgrunnlag, nyttBeregningsgrunnlag);

        BeregningsgrunnlagPeriode nyBgPeriode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);

        // Assert
        assertThat(nyBgPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(3);
        BeregningsgrunnlagPrStatusOgAndel eksisterendeBrukersAndel = nyBgPeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream().filter(andel -> andel.getAndelsnr().equals(1L)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel eksisterendeATAndel = nyBgPeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream().filter(andel -> andel.getAndelsnr().equals(2L)).findFirst().get();
        BeregningsgrunnlagPrStatusOgAndel nyAndel = nyBgPeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream().filter(andel -> andel.getAndelsnr().equals(3L)).findFirst().get();

        assertThat(eksisterendeBrukersAndel.getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
        assertThat(eksisterendeBrukersAndel.getBeregnetPrÅr()).isEqualByComparingTo(BEREGNET_PR_ÅR);
        assertThat(eksisterendeBrukersAndel.getLagtTilAvSaksbehandler()).isFalse();
        assertThat(eksisterendeBrukersAndel.getAktivitetStatus()).isEqualByComparingTo(AktivitetStatus.BRUKERS_ANDEL);

        assertThat(eksisterendeATAndel.getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
        assertThat(eksisterendeATAndel.getBeregnetPrÅr()).isEqualByComparingTo(BEREGNET_PR_ÅR);
        assertThat(eksisterendeATAndel.getLagtTilAvSaksbehandler()).isFalse();
        assertThat(eksisterendeATAndel.getAktivitetStatus()).isEqualByComparingTo(AktivitetStatus.ARBEIDSTAKER);

        assertThat(nyAndel.getInntektskategori()).isEqualTo(Inntektskategori.SJØMANN);
        assertThat(nyAndel.getBeregnetPrÅr()).isEqualByComparingTo(BEREGNET_PR_ÅR);
        assertThat(nyAndel.getLagtTilAvSaksbehandler()).isTrue();
        assertThat(nyAndel.getAktivitetStatus()).isEqualByComparingTo(AktivitetStatus.BRUKERS_ANDEL);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlagKunYtelse(boolean overstyrteVerdier, boolean skalHaArbeidstakerAndel) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.KUN_YTELSE))
            .medSkjæringstidspunkt(LocalDate.now())
            .build();
        lagBeregningsgrunnlagPeriode(overstyrteVerdier, beregningsgrunnlag, skalHaArbeidstakerAndel);
        return beregningsgrunnlag;
    }

    private void lagBeregningsgrunnlagPeriode(boolean overstyrteVerdier, BeregningsgrunnlagEntitet beregningsgrunnlag, boolean skalHaArbeidstakerAndel) {
        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(LocalDate.now(), null)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
            .medInntektskategori(Inntektskategori.UDEFINERT);
        if (overstyrteVerdier) {
            andelBuilder.medBeregnetPrÅr(BEREGNET_PR_ÅR)
                .medOverstyrtPrÅr(OVERSTYRT_PR_ÅR)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medFastsattAvSaksbehandler(true)
                .medLagtTilAvSaksbehandler(false);
            BeregningsgrunnlagPrStatusOgAndel.Builder andellagtTil = BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
                .medInntektskategori(Inntektskategori.SJØMANN);
            andellagtTil.medBeregnetPrÅr(BEREGNET_PR_ÅR)
                .medOverstyrtPrÅr(OVERSTYRT_PR_ÅR)
                .medInntektskategori(Inntektskategori.SJØMANN)
                .medFastsattAvSaksbehandler(true)
                .medLagtTilAvSaksbehandler(true)
                .medAndelsnr(3L);
            andellagtTil.build(beregningsgrunnlagPeriode);
        }
        andelBuilder.build(beregningsgrunnlagPeriode);
        if (skalHaArbeidstakerAndel) {
            BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder2 = BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAndelsnr(2L)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.fra(AktørId.dummy())))
                .medInntektskategori(Inntektskategori.UDEFINERT);
            if (overstyrteVerdier) {
                andelBuilder2.medBeregnetPrÅr(BEREGNET_PR_ÅR)
                    .medOverstyrtPrÅr(OVERSTYRT_PR_ÅR)
                    .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                    .medFastsattAvSaksbehandler(true)
                    .medLagtTilAvSaksbehandler(false);
            }
            andelBuilder2.build(beregningsgrunnlagPeriode);
        }
    }
}
