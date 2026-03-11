package no.nav.ung.ytelse.aktivitetspenger.beregning;

import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;

import java.util.ArrayList;
import java.util.List;

class AktivitetspengerBeregningsgrunnlagBuilder {

    private List<Beregningsgrunnlag> beregningsgrunnlag;

    private boolean built = false;

    AktivitetspengerBeregningsgrunnlagBuilder(AktivitetspengerBeregningsgrunnlag kladd) {
        if (kladd != null) {
            this.beregningsgrunnlag = new ArrayList<>(kladd.getBeregningsgrunnlag());
        } else {
            this.beregningsgrunnlag = new ArrayList<>();
        }
    }

    AktivitetspengerBeregningsgrunnlagBuilder leggTilBeregningsgrunnlag(Beregningsgrunnlag beregningsgrunnlag) {
        this.beregningsgrunnlag.add(beregningsgrunnlag);
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


    private void validerState() {
        if (built) {
            throw new IllegalStateException("[Utviklerfeil] Skal ikke gjenbruke builder!");
        }
    }
}
