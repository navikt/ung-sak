package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.beregning.OppdaterKvoteTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@ApplicationScoped
public class PSBOppdaterKvoteTjeneste implements OppdaterKvoteTjeneste {

    @Override
    public void oppdaterKvote(BehandlingReferanse referanse) {
        return;
    }

}
