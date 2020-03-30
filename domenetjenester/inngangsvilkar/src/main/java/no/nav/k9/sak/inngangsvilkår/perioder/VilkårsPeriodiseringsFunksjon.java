package no.nav.k9.sak.inngangsvilkår.perioder;

import java.util.Set;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface VilkårsPeriodiseringsFunksjon {
    Set<DatoIntervallEntitet> utledPeriode(Long behandlingId);
}
