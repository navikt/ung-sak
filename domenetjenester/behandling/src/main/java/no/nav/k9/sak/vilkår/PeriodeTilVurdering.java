package no.nav.k9.sak.vilkår;

import java.time.LocalDate;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class PeriodeTilVurdering implements Comparable<PeriodeTilVurdering> {

    private final DatoIntervallEntitet periode;
    private boolean erForlengelse;
    private boolean erEndringIUttak;

    public PeriodeTilVurdering(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public LocalDate getSkjæringstidspunkt() {
        return periode.getFomDato();
    }

    public void setErForlengelse(boolean erForlengelse) {
        this.erForlengelse = erForlengelse;
    }

    public void setErEndringIUttak(boolean erEndringIUttak) {
        this.erEndringIUttak = erEndringIUttak;
    }

    public boolean erForlengelse() {
        return erForlengelse;
    }

    public boolean erEndringIUttak() {
        return erEndringIUttak;
    }

    @Override
    public String toString() {
        return "PeriodeTilVurdering{" +
            "periode=" + periode +
            ", erForlengelse=" + erForlengelse +
            ", erEndringIUttak=" + erEndringIUttak +
            '}';
    }

    @Override
    public int compareTo(PeriodeTilVurdering other) {
        return this.getPeriode().compareTo(other.periode);
    }
}
