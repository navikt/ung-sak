package no.nav.k9.sak.vilkår;

import java.util.NavigableSet;
import java.util.TreeSet;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class IngenEndringIUttakPeriode implements EndringIUttakPeriodeUtleder {

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(BehandlingReferanse behandlingReferanse) {
        return new TreeSet<>();
    }
}
