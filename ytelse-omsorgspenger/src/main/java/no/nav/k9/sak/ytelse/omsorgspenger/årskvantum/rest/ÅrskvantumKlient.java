package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

import java.util.UUID;

public interface ÅrskvantumKlient {

    ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumRequest årskvantumRequest);

    void avbrytÅrskvantumForBehandling(UUID behandlingUUID);

    ÅrskvantumResultat hentÅrskvantumForBehandling(UUID behandlingUUID);

    ÅrskvantumResultat hentÅrskvantumForFagsak(String saksnummer);

    ÅrskvantumResterendeDager hentResterendeKvantum(String aktørId);

}
