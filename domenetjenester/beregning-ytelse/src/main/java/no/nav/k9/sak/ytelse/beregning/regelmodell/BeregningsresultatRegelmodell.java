package no.nav.k9.sak.ytelse.beregning.regelmodell;

import java.util.List;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;

@RuleDocumentationGrunnlag
public class BeregningsresultatRegelmodell {
    private List<Beregningsgrunnlag> beregningsgrunnlag;
    private UttakResultat uttakResultat;
    private boolean skalVurdereOmArbeidsforholdGjelderFor;

    public BeregningsresultatRegelmodell(Beregningsgrunnlag beregningsgrunnlag, UttakResultat uttakResultat) {
        this.beregningsgrunnlag = List.of(beregningsgrunnlag);
        this.uttakResultat = uttakResultat;
    }

    public BeregningsresultatRegelmodell(List<Beregningsgrunnlag> beregningsgrunnlag, UttakResultat uttakResultat, boolean skalVurdereOmArbeidsforholdGjelderFor) {
        this.beregningsgrunnlag = beregningsgrunnlag;
        this.uttakResultat = uttakResultat;
        this.skalVurdereOmArbeidsforholdGjelderFor = skalVurdereOmArbeidsforholdGjelderFor;
    }

    public List<Beregningsgrunnlag> getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    public UttakResultat getUttakResultat() {
        return uttakResultat;
    }

    public boolean getSkalVurdereGjelderFor() {
        return skalVurdereOmArbeidsforholdGjelderFor;
    }
}
