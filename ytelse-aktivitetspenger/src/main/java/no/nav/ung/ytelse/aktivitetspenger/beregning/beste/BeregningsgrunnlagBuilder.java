package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.sak.diff.DiffEntity;

import java.math.BigDecimal;

class BeregningsgrunnlagBuilder {

    private BeregningInput beregningInput;
    private BigDecimal årsinntektSisteÅr;
    private BigDecimal årsinntektSisteTreÅr;
    private BigDecimal årsinntektBesteBeregning;
    private String regelSporing;

    private boolean built = false;

    BeregningsgrunnlagBuilder(Beregningsgrunnlag kladd) {
        if (kladd != null) {
            this.årsinntektSisteÅr = kladd.getÅrsinntektAvkortetOppjustertSisteÅr();
            this.årsinntektSisteTreÅr = kladd.getÅrsinntektAvkortetOppjustertSisteTreÅr();
            this.årsinntektBesteBeregning = kladd.getÅrsinntektAvkortetOppjustertBesteBeregning();
            this.regelSporing = kladd.getRegelSporing();
        }
    }

    BeregningsgrunnlagBuilder medResultat(BesteberegningResultat resultat) {
        this.beregningInput = resultat.getBeregningInput();
        this.årsinntektSisteÅr = resultat.getÅrsinntektSisteÅr();
        this.årsinntektSisteTreÅr = resultat.getÅrsinntektSisteTreÅr();
        this.årsinntektBesteBeregning = resultat.getÅrsinntektBesteBeregning();
        this.regelSporing = resultat.getRegelSporing();
        return this;
    }

    Beregningsgrunnlag build() {
        validerState();
        this.built = true;
        return repeatableBuild();
    }

    private Beregningsgrunnlag repeatableBuild() {
        return new Beregningsgrunnlag(beregningInput, årsinntektSisteÅr, årsinntektSisteTreÅr, årsinntektBesteBeregning, regelSporing);
    }

    boolean erForskjellig(Beregningsgrunnlag grunnlag, DiffEntity differ) {
        return differ.areDifferent(grunnlag, repeatableBuild());
    }

    private void validerState() {
        if (built) {
            throw new IllegalStateException("[Utviklerfeil] Skal ikke gjenbruke builder!");
        }
    }
}
