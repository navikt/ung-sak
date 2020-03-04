package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

@ApplicationScoped
public class BeregningInfotrygdsakTjeneste {

    protected BeregningInfotrygdsakTjeneste() {
        // for CDI proxy
    }

    public boolean vurderOgOppdaterSakSomBehandlesAvInfotrygd(@Deprecated Behandling behandling, BehandlingReferanse ref) {
        // FIXME K9 : trengs denne?
        return false;
    }

}
