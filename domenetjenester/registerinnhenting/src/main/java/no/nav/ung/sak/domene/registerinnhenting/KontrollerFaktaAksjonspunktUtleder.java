package no.nav.ung.sak.domene.registerinnhenting;

import java.util.List;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

public interface KontrollerFaktaAksjonspunktUtleder {

    List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref);

    List<AksjonspunktResultat> utledAksjonspunkterTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType);

}
