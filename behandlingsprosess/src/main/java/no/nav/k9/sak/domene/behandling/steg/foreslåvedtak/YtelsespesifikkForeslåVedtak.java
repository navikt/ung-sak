package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;

public interface YtelsespesifikkForeslåVedtak {

    BehandleStegResultat run(BehandlingReferanse ref);
        
}
