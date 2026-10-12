package no.nav.ung.sak.web.server.abac;

import java.util.Set;
import java.util.UUID;


public class UkjentBehandlingException extends UkjentAbacVerdiException {

    public UkjentBehandlingException(Long behandlingId) {
        super("Behandlingen med id " + behandlingId + " finnes ikke i applikasjonen.");
    }

    public UkjentBehandlingException(Set<UUID> behandlingUuid) {
        super("Minst en av behandlingen med uuid " + behandlingUuid + " finnes ikke i applikasjonen.");
    }
}
