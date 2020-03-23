package no.nav.k9.sak.inngangsvilkaar.perioder;

import java.util.Set;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

interface VilkårsPeriodiseringsFunksjon {
    Set<DatoIntervallEntitet> utledPeriode(Long behandlingId);
}
