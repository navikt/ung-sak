package no.nav.k9.sak.ytelse.beregning.regelmodell;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;

@RuleDocumentationGrunnlag
public class BeregningsresultatRegelmodell {
    private Beregningsgrunnlag beregningsgrunnlag;
    private UttakResultat uttakResultat;

    public BeregningsresultatRegelmodell(Beregningsgrunnlag beregningsgrunnlag, UttakResultat uttakResultat) {
        this.beregningsgrunnlag = beregningsgrunnlag;
        this.uttakResultat = uttakResultat;
    }

    public Beregningsgrunnlag getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    public UttakResultat getUttakResultat() {
        return uttakResultat;
    }
}
