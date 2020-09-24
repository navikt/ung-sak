package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;

import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface ErEndringIBeregningVurderer {
    Map<LocalDate, Boolean> vurderUgunst(BehandlingReferanse original, BehandlingReferanse revurdering, NavigableSet<LocalDate> skjæringstidspunkter);
}
