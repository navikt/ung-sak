package no.nav.k9.sak.registerendringer;

import no.nav.k9.sak.behandling.BehandlingReferanse;

public class IngenRelevanteEndringer implements RelevanteIAYRegisterendringerUtleder {

    @Override
    public EndringerIAY utledRelevanteEndringer(BehandlingReferanse behandlingReferanse) {
        return EndringerIAY.INGEN_RELEVANTE_ENDRINGER;
    }
}
