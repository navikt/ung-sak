package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
/**
 Hjelpeklasse som utleder hvilket tall som best representerer årsinntekt for beregningsgrunnlaget da GUI idag kun støtter visning for et slikt tall selv om grunnlaget er periodisert
 */
public final class FinnÅrsinntektvisningstall {

    static Optional<BigDecimal> finn(BeregningsgrunnlagEntitet beregningsgrunnlag) {

        if (beregningsgrunnlag.getBeregningsgrunnlagPerioder().isEmpty()) {
            return Optional.empty();
        }

        if (harStatusKunYtelse(beregningsgrunnlag)) {
            return Optional.ofNullable(førstePeriode(beregningsgrunnlag).getBruttoPrÅr());
        }

        if (erSelvstendigNæringsdrivende(beregningsgrunnlag)) {

            if  (harBesteberegningtilfelle(beregningsgrunnlag)) {
                return Optional.ofNullable(førstePeriode(beregningsgrunnlag).getBruttoPrÅr());
            }

            return finnBeregnetÅrsinntektVisningstallSelvstendigNæringsdrivende(beregningsgrunnlag);
        }

        return Optional.ofNullable(førstePeriode(beregningsgrunnlag).getBeregnetPrÅr());
    }

    private static Optional<BigDecimal> finnBeregnetÅrsinntektVisningstallSelvstendigNæringsdrivende(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        Optional<BeregningsgrunnlagPrStatusOgAndel> snAndelOpt = førstePeriode(beregningsgrunnlag).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> a.getAktivitetStatus().erSelvstendigNæringsdrivende())
            .findFirst();

        if (snAndelOpt.isPresent()) {
            BeregningsgrunnlagPrStatusOgAndel snAndel = snAndelOpt.get();

            if (snAndel.getNyIArbeidslivet() == null || Boolean.FALSE.equals(snAndel.getNyIArbeidslivet())) {
                return Optional.ofNullable(snAndel.getPgiSnitt());
            }
        }
        return Optional.empty();
    }

    private static boolean harBesteberegningtilfelle(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE);
    }

    private static boolean erSelvstendigNæringsdrivende(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getAktivitetStatuser().stream()
            .anyMatch(a -> a.getAktivitetStatus().erSelvstendigNæringsdrivende());
    }

    private static BeregningsgrunnlagPeriode førstePeriode(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
    }

    private static boolean harStatusKunYtelse(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getAktivitetStatuser().stream()
            .anyMatch(a -> AktivitetStatus.KUN_YTELSE.equals(a.getAktivitetStatus()));
    }
}
