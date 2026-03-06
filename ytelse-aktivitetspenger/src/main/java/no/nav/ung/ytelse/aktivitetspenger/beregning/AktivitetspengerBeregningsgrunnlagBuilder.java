package no.nav.ung.ytelse.aktivitetspenger.beregning;

import no.nav.ung.sak.diff.DiffEntity;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;

class AktivitetspengerBeregningsgrunnlagBuilder {

    private Beregningsgrunnlag beregningsgrunnlag;

    private boolean built = false;

    AktivitetspengerBeregningsgrunnlagBuilder(AktivitetspengerBeregningsgrunnlag kladd) {
        if (kladd != null) {
            this.beregningsgrunnlag = kladd.getBeregningsgrunnlag();
        }
    }

    AktivitetspengerBeregningsgrunnlagBuilder medBeregningsgrunnlag(Beregningsgrunnlag beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
        return this;
    }

    AktivitetspengerBeregningsgrunnlag build() {
        validerState();
        this.built = true;
        return repeatableBuild();
    }

    private AktivitetspengerBeregningsgrunnlag repeatableBuild() {
        AktivitetspengerBeregningsgrunnlag resultat = new AktivitetspengerBeregningsgrunnlag();
        resultat.setBeregningsgrunnlag(beregningsgrunnlag);
        return resultat;
    }


    boolean erForskjellig(AktivitetspengerBeregningsgrunnlag grunnlag, DiffEntity differ) {
        return differ.areDifferent(grunnlag, repeatableBuild());
    }

    private void validerState() {
        if (built) {
            throw new IllegalStateException("[Utviklerfeil] Skal ikke gjenbruke builder!");
        }
    }
}

