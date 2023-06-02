package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import java.time.LocalDateTime;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface SaksbehandlingsfristUtleder {
    LocalDateTime utledFrist(Behandling behandlingId);
}
