package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import java.util.UUID;

@RequestScoped
@Alternative
public class ÅrskvantumInMemoryKlient implements ÅrskvantumKlient {

    private ÅrskvantumResultat årskvantumResultat;

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumRequest årskvantumRequest) {
        return årskvantumResultat;
    }

    @Override
    public void avbrytÅrskvantumForBehandling(UUID behandlingUUID) {

    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumForBehandling(UUID behandlingUUID) {
        return null;
    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumForFagsak(String saksnummer) {
        return null;
    }

    @Override
    public ÅrskvantumResterendeDager hentResterendeKvantum(String aktørId) {
        return null;
    }

    public void setÅrskvantumResultat(ÅrskvantumResultat årskvantumResultat) {
        this.årskvantumResultat = årskvantumResultat;

    }
}
