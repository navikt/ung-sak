package no.nav.k9.sak.ytelse.beregning;


import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Ingen oppdatering av kvote
 */
@FagsakYtelseTypeRef
@ApplicationScoped
public class DefaultOppdaterKvoteTjeneste implements OppdaterKvoteTjeneste {


    @Override
    public void oppdaterKvote(BehandlingReferanse referanse) {
        // NO-OP
    }
}
