package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

public interface ÅrskvantumTjeneste {

    ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumRequest årskvantumRequest);

}
