package no.nav.k9.sak.registerendringer;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

public interface RelevanteIAYRegisterendringerUtleder {

    static RelevanteIAYRegisterendringerUtleder finnTjeneste(Instance<RelevanteIAYRegisterendringerUtleder> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(RelevanteIAYRegisterendringerUtleder.class, instances, ytelseType)
            .orElse(new IngenRelevanteEndringer());
    }


    EndringerIAY utledRelevanteEndringer(BehandlingReferanse behandlingReferanse);

}
