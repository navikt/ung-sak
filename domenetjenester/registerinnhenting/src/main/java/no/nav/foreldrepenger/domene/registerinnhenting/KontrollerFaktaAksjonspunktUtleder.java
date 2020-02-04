package no.nav.foreldrepenger.domene.registerinnhenting;

import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public interface KontrollerFaktaAksjonspunktUtleder {

    List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref);

    List<AksjonspunktResultat> utledAksjonspunkterTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType);

    boolean skalOverstyringLøsesTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType, AksjonspunktDefinisjon aksjonspunktDefinisjon);

}
