package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.ung.sak.diff.DiffEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

class BeregningsgrunnlagBuilder {

    private LocalDate virkningsdato;
    private BigDecimal årsinntektSisteÅr;
    private BigDecimal årsinntektSisteTreÅr;
    private BigDecimal årsinntektBesteBeregning;
    private String regelInput;
    private String regelSporing;

    private boolean built = false;

    BeregningsgrunnlagBuilder(Beregningsgrunnlag kladd) {
        if (kladd != null) {
            this.virkningsdato = kladd.getVirkningsdato();
            this.årsinntektSisteÅr = kladd.getÅrsinntektSisteÅr();
            this.årsinntektSisteTreÅr = kladd.getÅrsinntektSisteTreÅr();
            this.årsinntektBesteBeregning = kladd.getÅrsinntektBesteBeregning();
            this.regelInput = kladd.getRegelInput();
            this.regelSporing = kladd.getRegelSporing();
        }
    }

    BeregningsgrunnlagBuilder medResultat(BesteberegningResultat resultat) {
        this.virkningsdato = resultat.getBeregningInput().virkningsdato();
        this.årsinntektSisteÅr = resultat.getÅrsinntektSisteÅr();
        this.årsinntektSisteTreÅr = resultat.getÅrsinntektSisteTreÅr();
        this.årsinntektBesteBeregning = resultat.getÅrsinntektBesteBeregning();
        this.regelInput = resultat.getRegelInput();
        this.regelSporing = resultat.getRegelSporing();
        return this;
    }

    Beregningsgrunnlag build() {
        validerState();
        this.built = true;
        return repeatableBuild();
    }

    private Beregningsgrunnlag repeatableBuild() {
        return new Beregningsgrunnlag(virkningsdato, årsinntektSisteÅr, årsinntektSisteTreÅr, årsinntektBesteBeregning, regelInput, regelSporing);
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
