package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;

class FastsettBehandlingsresultatVedAvslagPåAvslag {

    private FastsettBehandlingsresultatVedAvslagPåAvslag() {
    }

    public static boolean vurder(Optional<Behandlingsresultat> resRevurdering, Optional<Behandlingsresultat> resOriginal, BehandlingType originalBehandlingType) {
        if (resOriginal.isPresent() && resRevurdering.isPresent()) {
            if (BehandlingType.FØRSTEGANGSSØKNAD.equals(originalBehandlingType)) {
                return erAvslagPåAvslag(resRevurdering.get(), resOriginal.get());
            }
        }
        return false;
    }

    public static Behandlingsresultat fastsett(Behandling revurdering) {
        return FastsettBehandlingsresultatVedEndring.buildBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING,
            List.of(KonsekvensForYtelsen.INGEN_ENDRING), Vedtaksbrev.INGEN);
    }

    private static boolean erAvslagPåAvslag(Behandlingsresultat resRevurdering, Behandlingsresultat resOriginal) {
        return false;
    }
}
