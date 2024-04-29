package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering;

import java.util.Objects;
import java.util.Set;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;

public class InntektsmeldingerOgPerioderCacheEntry {

    private Set<JournalpostId> journalpostIder;
    private Set<DatoIntervallEntitet> perioder;

    public InntektsmeldingerOgPerioderCacheEntry(Set<JournalpostId> journalpostId, Set<DatoIntervallEntitet> periode) {
        this.journalpostIder = Objects.requireNonNull(journalpostId);
        this.perioder = periode;
    }

    public Set<JournalpostId> getJournalpostIder() {
        return journalpostIder;
    }

    public Set<DatoIntervallEntitet> getPerioder() {
        return perioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InntektsmeldingerOgPerioderCacheEntry that = (InntektsmeldingerOgPerioderCacheEntry) o;
        return Objects.equals(journalpostIder, that.journalpostIder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostIder);
    }

    @Override
    public String toString() {
        return "InntektsmeldingPeriodeCache{" +
            "journalpostIder=" + journalpostIder +
            ", periode=" + perioder +
            '}';
    }
}
