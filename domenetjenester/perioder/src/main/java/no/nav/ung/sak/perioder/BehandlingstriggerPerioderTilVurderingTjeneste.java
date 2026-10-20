package no.nav.ung.sak.perioder;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.inject.Instance;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;

public interface BehandlingstriggerPerioderTilVurderingTjeneste {

    static Optional<BehandlingstriggerPerioderTilVurderingTjeneste> finnTjeneste(Instance<BehandlingstriggerPerioderTilVurderingTjeneste> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(BehandlingstriggerPerioderTilVurderingTjeneste.class, instances, ytelseType, behandlingType);
    }

    Map<BehandlingÅrsakType, LocalDateTimeline<Boolean>> utledPerTrigger(Long behandlingId, VilkårType vilkårType);
}
