package no.nav.k9.sak.skjæringstidspunkt;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

public interface SkjæringstidspunktTjeneste {

    Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId);

}
