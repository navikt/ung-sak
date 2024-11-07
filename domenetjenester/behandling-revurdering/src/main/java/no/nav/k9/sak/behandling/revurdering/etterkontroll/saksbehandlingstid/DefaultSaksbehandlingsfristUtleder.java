package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandling.saksbehandlingstid.SaksbehandlingsfristUtleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultSaksbehandlingsfristUtleder implements SaksbehandlingsfristUtleder {
    @Override
    public Optional<LocalDateTime> utledFrist(Behandling behandling) {
        return Optional.empty();
    }
}
