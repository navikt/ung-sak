package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

public interface ÅrskvantumKlient {

    ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumRequest årskvantumRequest);

    void avbrytÅrskvantumForBehandling(String behandlingId);

    ÅrskvantumResultat hentÅrskvantumForBehandling(String behandlingId);

    ÅrskvantumResultat hentÅrskvantumForFagsak(String saksnummer);

    ÅrskvantumResterendeDager hentResterendeKvantum(String aktørId);

}
