package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.komponenttest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;

class KomponenttestBeregningAssertUtil {


    static void assertBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag,
                                         LocalDate skjæringstidspunktForBeregning,
                                         List<AktivitetStatus> aktivitetStatuser) {
        assertThat(beregningsgrunnlag.getSkjæringstidspunkt()).isEqualTo(skjæringstidspunktForBeregning);
        assertThat(beregningsgrunnlag.getGrunnbeløp() != null).isTrue();
        assertThat(beregningsgrunnlag.getAktivitetStatuser()).hasSize(aktivitetStatuser.size());
        for (int i = 0; i < aktivitetStatuser.size(); i++) {
            assertThat(beregningsgrunnlag.getAktivitetStatuser().get(i).getAktivitetStatus()).isEqualTo(aktivitetStatuser.get(i));
        }
    }

    static void assertBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode,
                                                ÅpenDatoIntervallEntitet periode,
                                                BigDecimal beregnetPrÅr, Long dagsats, BigDecimal overstyrtPrÅr, BigDecimal refusjonskravPrÅr) {
        assertThat(beregningsgrunnlagPeriode.getDagsats()).isEqualTo(dagsats);
        if (overstyrtPrÅr != null) {
            assertThat(beregningsgrunnlagPeriode.getBruttoPrÅr()).isEqualByComparingTo(overstyrtPrÅr);
        }
        if (refusjonskravPrÅr == null){
            assertThat(beregningsgrunnlagPeriode.getTotaltRefusjonkravIPeriode().erNulltall()).isTrue();

        } else {
            assertThat(beregningsgrunnlagPeriode.getTotaltRefusjonkravIPeriode().getVerdi()).isEqualByComparingTo(refusjonskravPrÅr);
        }
        assertThat(beregningsgrunnlagPeriode.getBeregnetPrÅr()).isEqualByComparingTo(beregnetPrÅr);
        assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPeriodeFom()).isEqualTo(periode.getFomDato());
        if (periode.getTomDato() == null) {
            assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPeriodeTom()).isNull();
        } else {
            assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPeriodeTom()).isEqualTo(periode.getTomDato());
        }
    }

    static void assertBeregningsgrunnlagAndel(BeregningsgrunnlagPrStatusOgAndel andel,
                                              BigDecimal beregnetPrÅr, AktivitetStatus aktivitetStatus,
                                              Inntektskategori inntektskategori,
                                              LocalDate beregningsperiodFom,
                                              LocalDate beregningsperiodeTom, BigDecimal refusjonskravPrÅr, BigDecimal overstyrtPrÅr) {

        assertThat(andel.getOverstyrtPrÅr()).isEqualTo(overstyrtPrÅr);
        assertThat(andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .as("RefusjonskravPrÅr")
            .isEqualTo(refusjonskravPrÅr);
        if (beregnetPrÅr == null) {
            assertThat(andel.getBeregnetPrÅr()).as("BeregnetPrÅr").isNull();
        } else {
            assertThat(andel.getBeregnetPrÅr()).as("BeregnetPrÅr").isEqualByComparingTo(beregnetPrÅr);
        }
        assertThat(andel.getBeregningsperiodeFom()).isEqualTo(beregningsperiodFom);
        assertThat(andel.getBeregningsperiodeTom()).isEqualTo(beregningsperiodeTom);
        assertThat(andel.getAktivitetStatus()).isEqualTo(aktivitetStatus);
        assertThat(andel.getInntektskategori()).isEqualTo(inntektskategori);

    }

    static void assertSammenligningsgrunnlag(Sammenligningsgrunnlag sammenligningsgrunnlag,
                                             BigDecimal rapportertInntekt,
                                             Long avvik){

        assertThat(sammenligningsgrunnlag.getRapportertPrÅr()).isEqualTo(rapportertInntekt);
        assertThat(sammenligningsgrunnlag.getAvvikPromille()).isEqualTo(avvik);

    }
}
