package no.nav.ung.sak.vilkår;


import java.util.NavigableSet;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public interface EndringIUttakPeriodeUtleder {

    static EndringIUttakPeriodeUtleder finnTjeneste(Instance<EndringIUttakPeriodeUtleder> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(EndringIUttakPeriodeUtleder.class, instances, ytelseType)
            .orElse(new IngenEndringIUttakPeriode());
    }

    NavigableSet<DatoIntervallEntitet> utled(BehandlingReferanse behandlingReferanse);


}
