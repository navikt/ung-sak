package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.NavigableSet;
import java.util.TreeSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class Vurderingsdatoer {

    private NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
    private NavigableSet<LocalDate> datoerTilVurdering;
    private NavigableSet<DatoIntervallEntitet> forlengelser;

    public Vurderingsdatoer() {
        datoerTilVurdering = new TreeSet<>();
        perioderTilVurdering = new TreeSet<>();
        forlengelser = new TreeSet<>();
    }

    public Vurderingsdatoer(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, NavigableSet<LocalDate> datoerTilVurdering, NavigableSet<DatoIntervallEntitet> forlengelser) {
        this.perioderTilVurdering = perioderTilVurdering;
        this.datoerTilVurdering = datoerTilVurdering;
        this.forlengelser = forlengelser;
    }

    public NavigableSet<LocalDate> getDatoerTilVurdering() {
        return datoerTilVurdering;
    }

    public NavigableSet<DatoIntervallEntitet> getForlengelser() {
        return forlengelser;
    }

    public NavigableSet<DatoIntervallEntitet> getPerioderTilVurdering() {
        return perioderTilVurdering;
    }
}
