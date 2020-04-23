package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;

import java.util.UUID;

public interface ÅrskvantumKlient {

    ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumRequest årskvantumRequest);

    void avbrytÅrskvantumForBehandling(UUID behandlingUUID);

    ÅrskvantumResultat hentÅrskvantumForBehandling(UUID behandlingUUID);

    Periode hentPeriodeForFagsak(String saksnummer);

    ÅrskvantumResterendeDager hentResterendeKvantum(String aktørId);

}
