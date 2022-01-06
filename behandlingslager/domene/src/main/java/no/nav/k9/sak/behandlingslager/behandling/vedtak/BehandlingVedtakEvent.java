package no.nav.k9.sak.behandlingslager.behandling.vedtak;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingEvent;
import no.nav.k9.sak.typer.AktørId;

public class BehandlingVedtakEvent implements BehandlingEvent {
    private BehandlingVedtak vedtak;
    private Behandling behandling;

    public BehandlingVedtakEvent(BehandlingVedtak vedtak, Behandling behandling) {
        this.vedtak = vedtak;
        this.behandling = behandling;
    }

    @Override
    public Long getFagsakId() {
        return behandling.getFagsakId();
    }

    @Override
    public AktørId getAktørId() {
        return behandling.getAktørId();
    }

    @Override
    public Long getBehandlingId() {
        return behandling.getId();
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public BehandlingVedtak getVedtak() {
        return vedtak;
    }
}
