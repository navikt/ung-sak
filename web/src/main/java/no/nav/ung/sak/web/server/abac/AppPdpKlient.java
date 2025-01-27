package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import no.nav.k9.felles.sikkerhet.abac.PdpKlient;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.Tilgangsbeslutning;
import no.nav.ung.sak.tilgangskontroll.PolicyDecisionPoint;

@Dependent
@Alternative
@Priority(1)
public class AppPdpKlient implements PdpKlient {

    private final PolicyDecisionPoint pdp;

    @Inject
    public AppPdpKlient(PolicyDecisionPoint pdp) {
        this.pdp = pdp;
    }

    @Override
    public Tilgangsbeslutning forespørTilgang(PdpRequest pdpRequest) {
        return pdp.vurderTilgangForInnloggetBruker(pdpRequest);
    }
}
