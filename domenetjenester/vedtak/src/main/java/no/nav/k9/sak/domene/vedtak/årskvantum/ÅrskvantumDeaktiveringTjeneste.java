package no.nav.k9.sak.domene.vedtak.årskvantum;

import java.util.UUID;


public interface ÅrskvantumDeaktiveringTjeneste {
    void deaktiverUttakForBehandling(UUID behandlingUuid);
}
