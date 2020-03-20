package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.StartpunktRef;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@StartpunktRef
@ApplicationScoped
class KontrollerFaktaTjeneste extends KontrollerFaktaTjenesteImpl {

    protected KontrollerFaktaTjeneste() {
        // for CDI proxy
    }

    @Inject
    KontrollerFaktaTjeneste(KontrollerFaktaUtledereTjenesteImpl utlederTjeneste,
                            BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(utlederTjeneste, behandlingskontrollTjeneste);
    }
}
