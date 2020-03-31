package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

@RequestScoped
@Alternative
public class ÅrskvantumInMemoryKlient implements ÅrskvantumKlient {

    private ÅrskvantumResultat årskvantumResultat;

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumRequest årskvantumRequest) {
        return årskvantumResultat;
    }

    public void setÅrskvantumResultat(ÅrskvantumResultat årskvantumResultat) {
        this.årskvantumResultat = årskvantumResultat;

    }
}
