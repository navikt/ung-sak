package no.nav.ung.sak.vilkår;

import java.util.NavigableSet;
import java.util.TreeSet;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

class IngenEndringIUttakPeriode implements EndringIUttakPeriodeUtleder {

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(BehandlingReferanse behandlingReferanse) {
        return new TreeSet<>();
    }
}
