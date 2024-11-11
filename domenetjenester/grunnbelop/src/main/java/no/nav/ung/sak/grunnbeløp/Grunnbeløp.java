package no.nav.ung.sak.grunnbeløp;

import jakarta.persistence.Entity;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity()
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
