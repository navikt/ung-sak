package no.nav.ung.sak.behandling.saksbehandlingstid;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface SaksbehandlingsfristUtleder {
    Optional<LocalDateTime> utledFrist(Behandling behandling);

    static SaksbehandlingsfristUtleder finnUtleder(
        Behandling behandling,
        Instance<SaksbehandlingsfristUtleder> fristUtledere) {
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(fristUtledere, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + SaksbehandlingsfristUtleder.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }
}
