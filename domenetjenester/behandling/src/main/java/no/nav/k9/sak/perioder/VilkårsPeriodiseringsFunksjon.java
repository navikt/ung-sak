package no.nav.k9.sak.perioder;

import java.util.NavigableSet;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface VilkårsPeriodiseringsFunksjon {
    NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId);
}
