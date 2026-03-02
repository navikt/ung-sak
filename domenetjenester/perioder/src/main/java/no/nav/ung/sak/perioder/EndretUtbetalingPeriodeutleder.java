package no.nav.ung.sak.perioder;

import java.util.NavigableSet;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public interface EndretUtbetalingPeriodeutleder {

    public static EndretUtbetalingPeriodeutleder finnUtleder(Instance<EndretUtbetalingPeriodeutleder> instances, FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(EndretUtbetalingPeriodeutleder.class, instances, fagsakYtelseType, behandlingType).orElseThrow(() -> new IllegalStateException("Har ikke EndretUtbetalingPeriodeutleder for " + fagsakYtelseType));
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode);

    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse);

}
