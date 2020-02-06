package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class FinnÅrsinntektvisningstallTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019,1,1);
    private static final String ORGNR = "987123987";

    @Test
    public void skal_ikke_sette_visningstall_hvis_ingen_perioder_på_grunnlaget() {
        BeregningsgrunnlagEntitet grunnlag = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        Optional<BigDecimal> visningstall = FinnÅrsinntektvisningstall.finn(grunnlag);

        assertThat(visningstall).isEmpty();
    }

    @Test
    public void skal_sette_visningstall_lik_brutto_første_periode() {
        BigDecimal bruttoFørstePeriode = BigDecimal.valueOf(500000);
        BeregningsgrunnlagEntitet grunnlag = lagBeregningsgrunnlagVanlig(bruttoFørstePeriode);

        Optional<BigDecimal> visningstall = FinnÅrsinntektvisningstall.finn(grunnlag);

        assertThat(visningstall).isPresent();
        assertThat(visningstall).hasValue(bruttoFørstePeriode);
    }

    @Test
    public void skal_sette_visningstall_lik_pgisnitt_hvis_selvstendig_næringsdrivende_og_ikke_ny_i_arblivet() {
        BigDecimal pgi= BigDecimal.valueOf(987595);
        BigDecimal brutto = BigDecimal.valueOf(766663);
        BeregningsgrunnlagEntitet grunnlag = lagBeregningsgrunnlagSN(pgi, brutto, false, false);

        Optional<BigDecimal> visningstall = FinnÅrsinntektvisningstall.finn(grunnlag);

        assertThat(visningstall).isPresent();
        assertThat(visningstall).hasValue(pgi);
    }

    @Test
    public void skal_ikke_returnere_visningstall_hvis_sn_mned_ny_i_arbliv() {
        BigDecimal pgi = BigDecimal.valueOf(987595);
        BigDecimal brutto = BigDecimal.valueOf(766663);
        BeregningsgrunnlagEntitet grunnlag = lagBeregningsgrunnlagSN(pgi, brutto, true, false);

        Optional<BigDecimal> visningstall = FinnÅrsinntektvisningstall.finn(grunnlag);

        assertThat(visningstall).isEmpty();
    }

    @Test
    public void skal_sette_visningstall_lik_brutto_hvis_sn_med_besteberegning() {
        BigDecimal pgi = BigDecimal.valueOf(987595);
        BigDecimal brutto = BigDecimal.valueOf(766663);

        BeregningsgrunnlagEntitet grunnlag = lagBeregningsgrunnlagSN(pgi, brutto, false, true);

        Optional<BigDecimal> visningstall = FinnÅrsinntektvisningstall.finn(grunnlag);

        assertThat(visningstall).isPresent();
        assertThat(visningstall).hasValue(brutto);
    }

    @Test
    public void skal_sette_visningstall_lik_brutto_hvis_sn_med_besteberegning_og_ny_i_arb() {
        BigDecimal pgi = BigDecimal.valueOf(987595);
        BigDecimal brutto = BigDecimal.valueOf(766663);

        BeregningsgrunnlagEntitet grunnlag = lagBeregningsgrunnlagSN(pgi, brutto, true, true);

        Optional<BigDecimal> visningstall = FinnÅrsinntektvisningstall.finn(grunnlag);

        assertThat(visningstall).isPresent();
        assertThat(visningstall).hasValue(brutto);
    }

    @Test
    public void skal_håndtere_nullverdier() {
        BeregningsgrunnlagEntitet grunnlag = lagBeregningsgrunnlagSN(null, null, false, false);

        Optional<BigDecimal> visningstall = FinnÅrsinntektvisningstall.finn(grunnlag);

        assertThat(visningstall).isEmpty();
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlagVanlig(BigDecimal bruttoFørstePeriode) {

        BeregningsgrunnlagEntitet grunnlag = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(grunnlag);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR)))
            .medBeregnetPrÅr(bruttoFørstePeriode)
            .build(aktivPeriode);

        return grunnlag;

    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlagSN(BigDecimal pgiSnitt, BigDecimal bruttoPrÅrAndel, boolean nyIArbliv, boolean medBesteberegning) {

        BeregningsgrunnlagEntitet grunnlag = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .leggTilFaktaOmBeregningTilfeller(medBesteberegning ? Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE) : Collections.emptyList())
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();

        BeregningsgrunnlagPeriode aktivPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(grunnlag);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBeregnetPrÅr(bruttoPrÅrAndel)
            .medPgi(pgiSnitt, Collections.emptyList())
            .medNyIArbeidslivet(nyIArbliv)
            .build(aktivPeriode);


        return grunnlag;

    }


}
