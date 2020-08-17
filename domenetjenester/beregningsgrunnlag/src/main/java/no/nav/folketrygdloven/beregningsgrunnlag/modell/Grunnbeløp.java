package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class Grunnbeløp {

    private long verdi;
    private DatoIntervallEntitet periode;

    public Grunnbeløp(long verdi, DatoIntervallEntitet periode) {
        this.verdi = verdi;
        this.periode = periode;
    }

    public long getVerdi() {
        return verdi;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
}
