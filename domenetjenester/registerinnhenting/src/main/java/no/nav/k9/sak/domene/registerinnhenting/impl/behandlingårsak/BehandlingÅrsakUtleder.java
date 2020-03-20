package no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Set;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface BehandlingÅrsakUtleder {
    Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);
}
