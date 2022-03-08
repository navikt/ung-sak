package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.util.List;

public class BeregningAktiviteterEndring {

    private final List<BeregningAktivitetEndring> aktivitetEndringer;

    public BeregningAktiviteterEndring(List<BeregningAktivitetEndring> aktivitetEndringer) {
        this.aktivitetEndringer = aktivitetEndringer;
    }

    public List<BeregningAktivitetEndring> getAktivitetEndringer() {
        return aktivitetEndringer;
    }
}
