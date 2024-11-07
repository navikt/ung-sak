package no.nav.k9.sak.vilkår;


import java.util.NavigableSet;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface EndringIUttakPeriodeUtleder {

    static EndringIUttakPeriodeUtleder finnTjeneste(Instance<EndringIUttakPeriodeUtleder> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(EndringIUttakPeriodeUtleder.class, instances, ytelseType)
            .orElse(new IngenEndringIUttakPeriode());
    }

    NavigableSet<DatoIntervallEntitet> utled(BehandlingReferanse behandlingReferanse);


}
