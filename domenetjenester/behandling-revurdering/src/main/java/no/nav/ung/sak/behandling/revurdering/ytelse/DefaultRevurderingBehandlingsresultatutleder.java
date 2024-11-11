package no.nav.ung.sak.behandling.revurdering.ytelse;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef(BehandlingType.REVURDERING)
public class DefaultRevurderingBehandlingsresultatutleder implements RevurderingBehandlingsresultatutleder {

    public DefaultRevurderingBehandlingsresultatutleder() {
    }

    @Override
    public void bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef) { }
}
