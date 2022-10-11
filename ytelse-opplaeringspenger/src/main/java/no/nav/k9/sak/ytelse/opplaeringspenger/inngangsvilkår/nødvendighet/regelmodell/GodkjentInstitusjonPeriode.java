package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.time.LocalDate;

public class GodkjentInstitusjonPeriode {

    private LocalDate fom;
    private LocalDate tom;
    private InstitusjonVurdering vurdering;

    public GodkjentInstitusjonPeriode(LocalDate fom, LocalDate tom, InstitusjonVurdering vurdering) {
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

    public InstitusjonVurdering getVurdering() {
        return vurdering;
    }
}
