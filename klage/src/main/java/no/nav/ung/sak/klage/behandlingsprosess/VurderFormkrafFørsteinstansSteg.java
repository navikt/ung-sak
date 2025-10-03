package no.nav.ung.sak.klage.behandlingsprosess;

import static java.util.Collections.singletonList;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.*;

@BehandlingStegRef(BehandlingStegType.VURDER_FORMKRAV_KLAGE_FØRSTEINSTANS)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderFormkrafFørsteinstansSteg implements BehandlingSteg {


    public VurderFormkrafFørsteinstansSteg(){
        // For CDI proxy
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        List<AksjonspunktDefinisjon> aksjonspunktDefinisjons = singletonList(AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_VEDTAKSINSTANS);
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjons);
    }
}
