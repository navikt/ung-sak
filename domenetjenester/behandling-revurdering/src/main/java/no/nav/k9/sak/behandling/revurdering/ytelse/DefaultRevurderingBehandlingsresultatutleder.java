package no.nav.k9.sak.behandling.revurdering.ytelse;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef(BehandlingType.REVURDERING)
public class DefaultRevurderingBehandlingsresultatutleder implements RevurderingBehandlingsresultatutleder {

    public DefaultRevurderingBehandlingsresultatutleder() {
    }

    @Override
    public void bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef) { }
}
