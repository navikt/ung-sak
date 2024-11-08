package no.nav.k9.sak.behandling.revurdering.ytelse;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

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
