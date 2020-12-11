package no.nav.k9.sak.kontrakt.behandling;


import no.nav.k9.kodeverk.behandling.BehandlingType;

public class BehandlingOpprettingDto {

    private BehandlingType behandlingType;
    private boolean kanOppretteBehandling;

    public BehandlingOpprettingDto(BehandlingType behandlingType, boolean kanOppretteBehandling) {
        this.behandlingType = behandlingType;
        this.kanOppretteBehandling = kanOppretteBehandling;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public boolean isKanOppretteBehandling() {
        return kanOppretteBehandling;
    }
}
