package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

public interface ÅrskvantumTjeneste {

    ÅrskvantumResultat hentÅrskvantumUttak(BehandlingReferanse ref);

}
