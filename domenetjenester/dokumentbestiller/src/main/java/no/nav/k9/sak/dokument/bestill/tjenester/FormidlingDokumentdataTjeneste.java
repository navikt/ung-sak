package no.nav.k9.sak.dokument.bestill.tjenester;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

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

    public void slettAllData(Long behandlingId) {
        formidlingDokumentdataKlient.slettAllData(behandlingRepository.hentBehandling(behandlingId).getUuid());
    }
}
