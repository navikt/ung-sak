package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

@RequestScoped
@Alternative
public class ÅrskvantumInMemoryKlient implements ÅrskvantumKlient {

    private ÅrskvantumResultat årskvantumResultat;

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumRequest årskvantumRequest) {
        return årskvantumResultat;
    }

    @Override
    public void avbrytÅrskvantumForBehandling(String behandlingId) {

    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumForBehandling(String behandlingId) {
        return null;
    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumForFagsak(String fagsakId) {
        return null;
    }

    @Override
    public ÅrskvantumRest hentResterendeKvantum(String aktørId) {
        return null;
    }

    public void setÅrskvantumResultat(ÅrskvantumResultat årskvantumResultat) {
        this.årskvantumResultat = årskvantumResultat;

    }
}
