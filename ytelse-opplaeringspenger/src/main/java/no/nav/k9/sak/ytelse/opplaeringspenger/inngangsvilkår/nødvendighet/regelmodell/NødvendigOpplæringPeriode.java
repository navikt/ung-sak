package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell;

import java.time.LocalDate;
import java.util.Objects;

public class NødvendigOpplæringPeriode {

    private final LocalDate fom;
    private final LocalDate tom;
    private final OpplæringVurdering vurdering;

    public NødvendigOpplæringPeriode(LocalDate fom, LocalDate tom, OpplæringVurdering vurdering) {
        Objects.requireNonNull(fom);
        Objects.requireNonNull(tom);
        Objects.requireNonNull(vurdering);
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

    public OpplæringVurdering getVurdering() {
        return vurdering;
    }
}
