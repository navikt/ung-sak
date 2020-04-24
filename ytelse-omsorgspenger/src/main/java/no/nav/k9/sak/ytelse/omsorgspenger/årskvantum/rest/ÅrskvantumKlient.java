package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import java.util.UUID;

import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;

public interface ÅrskvantumKlient {

    ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumRequest årskvantumRequest);

    void avbrytÅrskvantumForBehandling(UUID behandlingUUID);

    ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUUID);

    Periode hentPeriodeForFagsak(String saksnummer);

    ÅrskvantumResterendeDager hentResterendeKvantum(String aktørId);

}
