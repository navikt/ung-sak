package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ErEndringIBeregningVurderer;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBErEndringIBeregningVurderer implements ErEndringIBeregningVurderer {

    public PSBErEndringIBeregningVurderer() {
        // CDI
    }

    @Override
    public Map<LocalDate, Boolean> vurderUgunst(BehandlingReferanse orginalBeregning, BehandlingReferanse revurdering, NavigableSet<LocalDate> skjæringstidspunkter) {
        // mapper om output
        return skjæringstidspunkter.stream().collect(Collectors.toMap(v -> v, v -> false));
    }
}
