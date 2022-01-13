package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class PerioderTilVurdering {

    private final NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
    private final NavigableSet<DatoIntervallEntitet> forlengelseTilVurdering;

    public PerioderTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, NavigableSet<DatoIntervallEntitet> forlengelseTilVurdering) {
        this.perioderTilVurdering = Objects.requireNonNull(perioderTilVurdering);
        this.forlengelseTilVurdering = Objects.requireNonNull(forlengelseTilVurdering);
    }

    public Optional<LocalDate> getTidligsteDatoTilVurdering() {
        return perioderTilVurdering.stream()
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo);
    }

    public NavigableSet<DatoIntervallEntitet> getPerioderTilVurdering() {
        return perioderTilVurdering.stream()
            .filter(it -> !forlengelseTilVurdering.contains(it))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public NavigableSet<DatoIntervallEntitet> getForlengelseTilVurdering() {
        return forlengelseTilVurdering;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerioderTilVurdering that = (PerioderTilVurdering) o;
        return Objects.equals(perioderTilVurdering, that.perioderTilVurdering) && Objects.equals(forlengelseTilVurdering, that.forlengelseTilVurdering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioderTilVurdering, forlengelseTilVurdering);
    }

    @Override
    public String toString() {
        return "PerioderTilVurdering{" +
            "perioderTilVurdering=" + perioderTilVurdering +
            ", forlengelseTilVurdering=" + forlengelseTilVurdering +
            '}';
    }
}
