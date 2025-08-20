package no.nav.ung.sak.web.server.abac;

import no.nav.sif.abac.kontrakt.abac.AbacBehandlingStatus;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;

import java.util.Optional;

public final class AbacUtil {

    public static Optional<AbacFagsakStatus> oversettFagstatus(FagsakStatus fagsakStatus) {
        if (fagsakStatus == FagsakStatus.OPPRETTET) {
            return Optional.of(AbacFagsakStatus.OPPRETTET);
        } else if (fagsakStatus == FagsakStatus.UNDER_BEHANDLING) {
            return Optional.of(AbacFagsakStatus.UNDER_BEHANDLING);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<AbacBehandlingStatus> oversettBehandlingStatus(BehandlingStatus behandlingStatus) {
        if (behandlingStatus == BehandlingStatus.OPPRETTET) {
            return Optional.of(AbacBehandlingStatus.OPPRETTET);
        } else if (behandlingStatus == BehandlingStatus.UTREDES) {
            return Optional.of(AbacBehandlingStatus.UTREDES);
        } else if (behandlingStatus == BehandlingStatus.FATTER_VEDTAK) {
            return Optional.of(AbacBehandlingStatus.FATTE_VEDTAK);
        } else {
            return Optional.empty();
        }
    }

    private AbacUtil() {
        // util class
    }

}
