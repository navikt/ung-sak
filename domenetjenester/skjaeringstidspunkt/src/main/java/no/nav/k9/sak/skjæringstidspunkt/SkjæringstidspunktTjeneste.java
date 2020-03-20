package no.nav.k9.sak.skjæringstidspunkt;

import no.nav.k9.sak.behandling.Skjæringstidspunkt;

public interface SkjæringstidspunktTjeneste {

    Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId);

}
