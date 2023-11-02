package no.nav.k9.sak.ytelse.beregning;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

public interface OppdaterKvoteTjeneste {

    static OppdaterKvoteTjeneste finnTjeneste(Instance<OppdaterKvoteTjeneste> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(OppdaterKvoteTjeneste.class, instances, ytelseType)
            .orElse(new DefaultOppdaterKvoteTjeneste());
    }

    public void oppdaterKvote(BehandlingReferanse referanse);

}
