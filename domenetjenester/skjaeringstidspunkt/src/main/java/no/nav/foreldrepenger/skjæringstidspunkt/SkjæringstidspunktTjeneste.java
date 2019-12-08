package no.nav.foreldrepenger.skjæringstidspunkt;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

public interface SkjæringstidspunktTjeneste {

    Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId);

}
