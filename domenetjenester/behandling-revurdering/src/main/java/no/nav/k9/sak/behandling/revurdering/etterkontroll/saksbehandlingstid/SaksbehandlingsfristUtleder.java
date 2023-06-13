package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface SaksbehandlingsfristUtleder {
    Optional<LocalDateTime> utledFrist(Behandling behandlingId);
}
