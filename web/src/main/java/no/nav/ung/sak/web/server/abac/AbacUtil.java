package no.nav.ung.sak.web.server.abac;

import no.nav.sif.abac.kontrakt.abac.AbacBehandlingStatus;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;

import java.util.Optional;

public final class AbacUtil {

    public static Optional<AbacFagsakStatus> oversettFagstatus(FagsakStatus status) {
        if (status == FagsakStatus.OPPRETTET) {
            return Optional.of(AbacFagsakStatus.OPPRETTET);
        } else if (status == FagsakStatus.UNDER_BEHANDLING) {
            return Optional.of(AbacFagsakStatus.UNDER_BEHANDLING);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<AbacBehandlingStatus> oversettBehandlingStatus(BehandlingStatus status) {
        if (status == BehandlingStatus.OPPRETTET) {
            return Optional.of(AbacBehandlingStatus.OPPRETTET);
        } else if (status == BehandlingStatus.UTREDES) {
            return Optional.of(AbacBehandlingStatus.UTREDES);
        } else if (status == BehandlingStatus.FATTER_VEDTAK) {
            return Optional.of(AbacBehandlingStatus.FATTE_VEDTAK);
        } else {
            return Optional.empty();
        }
    }

    private AbacUtil() {
        // util class
    }

}
