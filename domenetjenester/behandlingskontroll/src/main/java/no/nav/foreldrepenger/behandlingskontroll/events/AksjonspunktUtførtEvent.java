package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;

public class AksjonspunktUtførtEvent extends AksjonspunktEvent {

    public AksjonspunktUtførtEvent(BehandlingskontrollKontekst kontekst, List<Aksjonspunkt> aksjonspunkter,
                                   BehandlingStegType behandlingStegType) {
        super(kontekst, aksjonspunkter, behandlingStegType);
    }
}
