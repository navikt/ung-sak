package no.nav.k9.sak.perioder;

import java.util.NavigableSet;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface EndretUtbetalingPeriodeutleder {

    public static EndretUtbetalingPeriodeutleder finnUtleder(Instance<EndretUtbetalingPeriodeutleder> instances, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(instances, fagsakYtelseType).orElseThrow(() -> new IllegalStateException("Har ikke EndretUtbetalingPeriodeutleder for " + fagsakYtelseType));
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode);

    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse);

}
