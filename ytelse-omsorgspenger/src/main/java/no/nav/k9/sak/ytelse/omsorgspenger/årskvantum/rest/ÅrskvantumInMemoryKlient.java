package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.ÅrskvantumTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;

@RequestScoped 
@Alternative
public class ÅrskvantumInMemoryKlient implements ÅrskvantumTjeneste {

    private ÅrskvantumResultat årskvantumResultat;

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(BehandlingReferanse ref) {
        return årskvantumResultat;
    }

    public void setÅrskvantumResultat(ÅrskvantumResultat årskvantumResultat) {
        this.årskvantumResultat = årskvantumResultat;
        
    }
}
