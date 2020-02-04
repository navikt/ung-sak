package no.nav.foreldrepenger.inngangsvilkaar.perioder;

import java.util.Set;

import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

interface VilkårsPeriodiseringsFunksjon {
    Set<DatoIntervallEntitet> utledPeriode(Long behandlingId);
}
