package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.time.LocalDate;

import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface ErEndringIBeregningVurderer {
    boolean vurderUgunst(BehandlingReferanse orginalBeregning, BehandlingReferanse revurdering, LocalDate skjæringstidspuntk);
}
