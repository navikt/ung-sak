package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.time.LocalDate;

public class GodkjentSykdomsvilkårPeriode {
    private LocalDate fom;
    private LocalDate tom;
    private SykdomVurdering vurdering;

    public GodkjentSykdomsvilkårPeriode(LocalDate fom, LocalDate tom, SykdomVurdering vurdering) {
        this.fom = fom;
        this.tom = tom;
        this.vurdering = vurdering;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public SykdomVurdering getVurdering() {
        return vurdering;
    }
}
