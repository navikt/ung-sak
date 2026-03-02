package no.nav.ung.sak.perioder;

import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

import java.util.Set;

public interface ProsessTriggerPeriodeUtleder {
    LocalDateTimeline<Set<BehandlingÅrsakType>> utledTidslinje(Long behandligId);

    static ProsessTriggerPeriodeUtleder finnTjeneste(Instance<ProsessTriggerPeriodeUtleder> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(ProsessTriggerPeriodeUtleder.class, instances, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType));
    }

}
