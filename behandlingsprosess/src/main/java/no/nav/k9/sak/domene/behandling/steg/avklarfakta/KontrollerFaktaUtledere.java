package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import java.util.List;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtleder;

public interface KontrollerFaktaUtledere {

    List<AksjonspunktUtleder> utledUtledereFor(BehandlingReferanse ref);
}
