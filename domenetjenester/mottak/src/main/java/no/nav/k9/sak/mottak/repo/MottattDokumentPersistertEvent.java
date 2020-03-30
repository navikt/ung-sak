package no.nav.k9.sak.mottak.repo;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingEvent;
import no.nav.k9.sak.typer.AktørId;

public class MottattDokumentPersistertEvent implements BehandlingEvent {
    private MottattDokument mottattDokument;
    private Behandling behandling;

    public MottattDokumentPersistertEvent(MottattDokument mottattDokument, Behandling behandling) {
        this.mottattDokument = mottattDokument;
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

    public MottattDokument getMottattDokument() {
        return mottattDokument;
    }
}
