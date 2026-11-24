package no.nav.ung.sak.behandlingslager.behandling.kontroll;

import java.util.Objects;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;

/**
 * Øyeblikksbilde av en åpen ytelsesbehandling, lest direkte fra databasen uten å gå via managed entitet.
 *
 * @param aktivtSteg steget behandlingen står i, eller null hvis behandlingen ikke er startet
 */
public record ParallellBehandling(Long behandlingId,
                                  BehandlingType behandlingType,
                                  BehandlingStatus status,
                                  BehandlingResultatType resultatType,
                                  BehandlingStegType aktivtSteg) {

    public ParallellBehandling {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(behandlingType, "behandlingType");
        Objects.requireNonNull(status, "status");
    }

    public boolean erHenlagt() {
        return resultatType != null && resultatType.erHenleggelse();
    }

    public boolean erKontrollbehandling() {
        return BehandlingType.KONTROLL == behandlingType;
    }
}
