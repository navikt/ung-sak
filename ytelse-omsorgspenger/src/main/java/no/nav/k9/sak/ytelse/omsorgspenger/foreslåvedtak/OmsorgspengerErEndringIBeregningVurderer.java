package no.nav.k9.sak.ytelse.omsorgspenger.foreslåvedtak;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ErEndringIBeregningVurderer;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerErEndringIBeregningVurderer implements ErEndringIBeregningVurderer {

    public OmsorgspengerErEndringIBeregningVurderer() {
    }

    @Override
    public boolean vurderUgunst(BehandlingReferanse orginalBeregning, BehandlingReferanse revurdering, LocalDate skjæringstidspuntk) {
        return false;
    }
}
