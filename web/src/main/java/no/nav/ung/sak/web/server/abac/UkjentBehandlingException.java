package no.nav.ung.sak.web.server.abac;

import java.util.Set;
import java.util.UUID;


public class UkjentBehandlingException extends UkjentAbacVerdiException {

    protected UkjentBehandlingException(Long behandlingId) {
        super("Behandlingen med id " + behandlingId + " finnes ikke i applikasjonen.");
    }

    protected UkjentBehandlingException(Set<UUID> behandlingUuid) {
        super("Minst en ab behandlingen med uuid " + behandlingUuid + " finnes ikke i applikasjonen.");
    }
}
