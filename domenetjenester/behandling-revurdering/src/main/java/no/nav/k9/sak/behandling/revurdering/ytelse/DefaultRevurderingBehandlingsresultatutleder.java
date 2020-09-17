package no.nav.k9.sak.behandling.revurdering.ytelse;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef("BT-004")
public class DefaultRevurderingBehandlingsresultatutleder implements RevurderingBehandlingsresultatutleder {

    public DefaultRevurderingBehandlingsresultatutleder() {
    }

    @Override
    public void bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef) { }
}
