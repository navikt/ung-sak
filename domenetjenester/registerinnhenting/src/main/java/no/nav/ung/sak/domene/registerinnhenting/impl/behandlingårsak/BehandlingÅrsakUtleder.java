package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;

public interface BehandlingÅrsakUtleder {
    Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);

    static Optional<BehandlingÅrsakUtleder> finnUtleder(Instance<BehandlingÅrsakUtleder> utledere, Class<?> aggregat, FagsakYtelseType ytelseType) {
        return GrunnlagRef.Lookup.find(BehandlingÅrsakUtleder.class, utledere, aggregat, ytelseType);
    }

}
