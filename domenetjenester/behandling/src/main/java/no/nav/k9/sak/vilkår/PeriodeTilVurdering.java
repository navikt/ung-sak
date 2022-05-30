package no.nav.k9.sak.vilkår;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class PeriodeTilVurdering implements Comparable<PeriodeTilVurdering> {

    private final DatoIntervallEntitet periode;
    private boolean erForlengelse;

    public PeriodeTilVurdering(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public void setErForlengelse(boolean erForlengelse) {
        this.erForlengelse = erForlengelse;
    }

    public boolean erForlengelse() {
        return erForlengelse;
    }

    @Override
    public String toString() {
        return "PeriodeTilVurdering{" +
            "periode=" + periode +
            ", erForlengelse=" + erForlengelse +
            '}';
    }

    @Override
    public int compareTo(PeriodeTilVurdering other) {
        return this.getPeriode().compareTo(other.periode);
    }
}
