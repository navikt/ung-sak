package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.NavigableSet;
import java.util.TreeSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class Vurderingsdatoer {

    private NavigableSet<LocalDate> datoerTilVurdering;
    private NavigableSet<DatoIntervallEntitet> forlengelser;

    public Vurderingsdatoer() {
        datoerTilVurdering = new TreeSet<>();
        forlengelser = new TreeSet<>();
    }

    public Vurderingsdatoer(NavigableSet<LocalDate> datoerTilVurdering, NavigableSet<DatoIntervallEntitet> forlengelser) {
        this.datoerTilVurdering = datoerTilVurdering;
        this.forlengelser = forlengelser;
    }

    public NavigableSet<LocalDate> getDatoerTilVurdering() {
        return datoerTilVurdering;
    }

    public NavigableSet<DatoIntervallEntitet> getForlengelser() {
        return forlengelser;
    }
}
