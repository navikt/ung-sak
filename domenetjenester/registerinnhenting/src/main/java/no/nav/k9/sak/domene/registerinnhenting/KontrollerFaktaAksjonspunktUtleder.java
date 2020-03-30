package no.nav.k9.sak.domene.registerinnhenting;

import java.util.List;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

public interface KontrollerFaktaAksjonspunktUtleder {

    List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref);

    List<AksjonspunktResultat> utledAksjonspunkterTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType);

}
