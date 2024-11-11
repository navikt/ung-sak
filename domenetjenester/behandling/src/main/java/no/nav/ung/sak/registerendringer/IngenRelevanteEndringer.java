package no.nav.ung.sak.registerendringer;

import no.nav.ung.sak.behandling.BehandlingReferanse;

public class IngenRelevanteEndringer implements RelevanteIAYRegisterendringerUtleder {

    @Override
    public EndringerIAY utledRelevanteEndringer(BehandlingReferanse behandlingReferanse) {
        return EndringerIAY.INGEN_RELEVANTE_ENDRINGER;
    }
}
