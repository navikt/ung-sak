package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Sjekk om revurdering endrer utfall.
 */
@FagsakYtelseTypeRef
@ApplicationScoped
public class RevurderingEndring {

    public RevurderingEndring() {
        // for CDI proxy
    }
}
