package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.sak.diff.DiffEntity;

import java.math.BigDecimal;

class BesteBeregningGrunnlagBuilder {

    private BigDecimal årsinntektSisteÅr;
    private BigDecimal årsinntektSisteTreÅr;
    private BigDecimal årsinntektBesteBeregning;
    private String regelInput;
    private String regelSporing;

    private boolean built = false;

    BesteBeregningGrunnlagBuilder(BesteBeregningGrunnlag kladd) {
        if (kladd != null) {
            this.årsinntektSisteÅr = kladd.getÅrsinntektSisteÅr();
            this.årsinntektSisteTreÅr = kladd.getÅrsinntektSisteTreÅr();
            this.årsinntektBesteBeregning = kladd.getÅrsinntektBesteBeregning();
            this.regelInput = kladd.getRegelInput();
            this.regelSporing = kladd.getRegelSporing();
        }
    }

    BesteBeregningGrunnlagBuilder medResultat(BesteBeregningResultat resultat) {
        this.årsinntektSisteÅr = resultat.getÅrsinntektSisteÅr();
        this.årsinntektSisteTreÅr = resultat.getÅrsinntektSisteTreÅr();
        this.årsinntektBesteBeregning = resultat.getÅrsinntektBesteBeregning();
        this.regelInput = resultat.getRegelInput();
        this.regelSporing = resultat.getRegelSporing();
        return this;
    }

    BesteBeregningGrunnlag build() {
        validerState();
        this.built = true;
        return repeatableBuild();
    }

    private BesteBeregningGrunnlag repeatableBuild() {
        return new BesteBeregningGrunnlag(årsinntektSisteÅr, årsinntektSisteTreÅr, årsinntektBesteBeregning, regelInput, regelSporing);
    }

    boolean erForskjellig(BesteBeregningGrunnlag grunnlag, DiffEntity differ) {
        return differ.areDifferent(grunnlag, repeatableBuild());
    }

    private void validerState() {
        if (built) {
            throw new IllegalStateException("[Utviklerfeil] Skal ikke gjenbruke builder!");
        }
    }
}
