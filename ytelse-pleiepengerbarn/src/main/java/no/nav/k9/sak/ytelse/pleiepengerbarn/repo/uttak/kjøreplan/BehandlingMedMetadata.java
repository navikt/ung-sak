package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import java.util.Optional;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;

public class BehandlingMedMetadata {
    private final BehandlingStatus behandlingStatus;
    private final Long orginalBehandling;

    public BehandlingMedMetadata(BehandlingStatus behandlingStatus, Long orginalBehandling) {
        this.behandlingStatus = behandlingStatus;
        this.orginalBehandling = orginalBehandling;
    }

    public BehandlingStatus getBehandlingStatus() {
        return behandlingStatus;
    }

    public Optional<Long> getOrginalBehandling() {
        return Optional.ofNullable(orginalBehandling);
    }
}
