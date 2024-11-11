package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;

public interface YtelsespesifikkForeslåVedtak {

    BehandleStegResultat run(BehandlingReferanse ref);

}
