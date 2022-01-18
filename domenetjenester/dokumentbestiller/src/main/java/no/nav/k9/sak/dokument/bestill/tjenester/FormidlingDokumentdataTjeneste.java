package no.nav.k9.sak.dokument.bestill.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.dokument.bestill.klient.FormidlingDokumentdataKlient;

@ApplicationScoped
@Default
public class FormidlingDokumentdataTjeneste {

    private BehandlingRepository behandlingRepository;
    private FormidlingDokumentdataKlient formidlingDokumentdataKlient;


    FormidlingDokumentdataTjeneste() {
    }

    @Inject
    public FormidlingDokumentdataTjeneste(BehandlingRepository behandlingRepository, FormidlingDokumentdataKlient formidlingDokumentdataKlient) {
        this.behandlingRepository = behandlingRepository;
        this.formidlingDokumentdataKlient = formidlingDokumentdataKlient;
    }

    public void ryddVedTilbakehopp(Long behandlingId) {
        formidlingDokumentdataKlient.ryddVedTilbakehopp(behandlingRepository.hentBehandling(behandlingId).getUuid());
    }
}
