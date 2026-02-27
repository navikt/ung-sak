package no.nav.ung.sak.web.server.abac;

import no.nav.sif.abac.kontrakt.abac.AbacBehandlingStatus;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakStatus;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakYtelseType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

import java.util.List;
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

    public static AbacFagsakYtelseType oversettYtelseType(FagsakYtelseType ytelsetype) {
        return switch (ytelsetype) {
            case UNGDOMSYTELSE -> AbacFagsakYtelseType.UNGDOMSYTELSE;
            case AKTIVITETSPENGER -> AbacFagsakYtelseType.AKTIVITETSPENGER;
            case OBSOLETE -> AbacFagsakYtelseType.OBSOLETE;
            default -> throw new IllegalArgumentException("Ikke-støttet verdi: " + ytelsetype);
        };
    }

    private AbacUtil() {
        // util class
    }

}
