package no.nav.ung.sak.registerendringer;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

public interface RelevanteIAYRegisterendringerUtleder {

    static RelevanteIAYRegisterendringerUtleder finnTjeneste(Instance<RelevanteIAYRegisterendringerUtleder> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(RelevanteIAYRegisterendringerUtleder.class, instances, ytelseType)
            .orElse(new IngenRelevanteEndringer());
    }


    EndringerIAY utledRelevanteEndringer(BehandlingReferanse behandlingReferanse);

}
