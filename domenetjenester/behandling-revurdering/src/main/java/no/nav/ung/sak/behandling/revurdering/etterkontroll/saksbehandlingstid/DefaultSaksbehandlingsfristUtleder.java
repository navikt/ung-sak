package no.nav.ung.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.behandling.saksbehandlingstid.SaksbehandlingsfristUtleder;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultSaksbehandlingsfristUtleder implements SaksbehandlingsfristUtleder {
    @Override
    public Optional<LocalDateTime> utledFrist(Behandling behandling) {
        return Optional.empty();
    }
}
