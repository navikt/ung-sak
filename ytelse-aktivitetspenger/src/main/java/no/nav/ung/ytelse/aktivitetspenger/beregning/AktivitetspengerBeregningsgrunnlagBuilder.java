package no.nav.ung.ytelse.aktivitetspenger.beregning;

import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPerioder;

import java.util.ArrayList;
import java.util.List;

class AktivitetspengerGrunnlagBuilder {

    private List<Beregningsgrunnlag> beregningsgrunnlag;
    private AktivitetspengerSatsPerioder grunnsatser;

    private boolean built = false;

    AktivitetspengerGrunnlagBuilder(AktivitetspengerGrunnlag kladd) {
        if (kladd != null) {
            this.beregningsgrunnlag = new ArrayList<>(kladd.getBeregningsgrunnlag());
            this.grunnsatser = kladd.getSatsperioder();
        } else {
            this.beregningsgrunnlag = new ArrayList<>();
        }
    }

    AktivitetspengerGrunnlagBuilder leggTilBeregningsgrunnlag(Beregningsgrunnlag beregningsgrunnlag) {
        this.beregningsgrunnlag.add(beregningsgrunnlag);
        return this;
    }

    AktivitetspengerGrunnlagBuilder medGrunnsatser(AktivitetspengerSatsPerioder grunnsatser) {
        this.grunnsatser = grunnsatser;
        return this;
    }

    AktivitetspengerGrunnlag build() {
        validerState();
        this.built = true;
        return repeatableBuild();
    }

    private AktivitetspengerGrunnlag repeatableBuild() {
        AktivitetspengerGrunnlag resultat = new AktivitetspengerGrunnlag();
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
