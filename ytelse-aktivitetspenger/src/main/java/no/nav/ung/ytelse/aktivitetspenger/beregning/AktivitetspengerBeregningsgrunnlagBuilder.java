package no.nav.ung.ytelse.aktivitetspenger.beregning;

import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minsteytelse.AktivitetspengerSatsPerioder;

import java.util.ArrayList;
import java.util.List;

class AktivitetspengerBeregningsgrunnlagBuilder {

    private List<Beregningsgrunnlag> beregningsgrunnlag;
    private AktivitetspengerSatsPerioder grunnsatser;

    private boolean built = false;

    AktivitetspengerBeregningsgrunnlagBuilder(AktivitetspengerBeregningsgrunnlag kladd) {
        if (kladd != null) {
            this.beregningsgrunnlag = new ArrayList<>(kladd.getBeregningsgrunnlag());
            this.grunnsatser = kladd.getSatsperioder();
        } else {
            this.beregningsgrunnlag = new ArrayList<>();
        }
    }

    AktivitetspengerBeregningsgrunnlagBuilder leggTilBeregningsgrunnlag(Beregningsgrunnlag beregningsgrunnlag) {
        this.beregningsgrunnlag.add(beregningsgrunnlag);
        return this;
    }

    AktivitetspengerBeregningsgrunnlagBuilder medGrunnsatser(AktivitetspengerSatsPerioder grunnsatser) {
        this.grunnsatser = grunnsatser;
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
        resultat.setSatsperioder(grunnsatser);
        return resultat;
    }


    private void validerState() {
        if (built) {
            throw new IllegalStateException("[Utviklerfeil] Skal ikke gjenbruke builder!");
        }
    }
}
