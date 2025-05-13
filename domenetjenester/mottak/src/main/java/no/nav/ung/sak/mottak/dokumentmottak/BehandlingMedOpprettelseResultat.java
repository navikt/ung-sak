package no.nav.ung.sak.mottak.dokumentmottak;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public record BehandlingMedOpprettelseResultat(Behandling behandling, boolean nyopprettet) {

    static BehandlingMedOpprettelseResultat nyBehandling(Behandling behandling) {
        return new BehandlingMedOpprettelseResultat(behandling, true);
    }

    static BehandlingMedOpprettelseResultat eksisterendeBehandling(Behandling behandling) {
        return new BehandlingMedOpprettelseResultat(behandling, false);
    }
}
