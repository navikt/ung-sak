package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;

/**
 * Brukes for å override bønnen for ForsinketSaksbehandlingEtterkontrollOppretter slik at andre CDI tester
 * ikke trigger den koden.
 */
@Alternative
@Priority(1)
public class DummyForsinketSaksbehandlingEtterkontrollOppretter implements ForsinketSaksbehandlingEtterkontrollOppretter {
    public void opprettEtterkontroll(Long behandlingId) {
    }
}
