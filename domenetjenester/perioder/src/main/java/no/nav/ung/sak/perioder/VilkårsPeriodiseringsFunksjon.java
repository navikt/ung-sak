package no.nav.ung.sak.perioder;

import java.util.NavigableSet;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public interface VilkårsPeriodiseringsFunksjon {
    NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId);
}
