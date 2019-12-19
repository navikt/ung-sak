package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef("VEDLEGG")
class DokumentmottakerVedlegg implements Dokumentmottaker {

    private BehandlingRepository behandlingRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private BehandlingRevurderingRepository revurderingRepository;
    private Kompletthetskontroller kompletthetskontroller;

    @Inject
    public DokumentmottakerVedlegg(BehandlingRepositoryProvider repositoryProvider,
                                   DokumentmottakerFelles dokumentmottakerFelles,
                                   Behandlingsoppretter behandlingsoppretter,
                                   Kompletthetskontroller kompletthetskontroller) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingsoppretter = behandlingsoppretter;
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.kompletthetskontroller = kompletthetskontroller;
    }

    @Override
    public void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, DokumentTypeId dokumentTypeId, BehandlingÅrsakType behandlingÅrsakType) {
        dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(mottattDokument.getFagsakId(), mottattDokument.getJournalpostId(), dokumentTypeId);

        Optional<Behandling> åpenBehandling = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .findFirst();

        if (åpenBehandling.isPresent()) {
            håndterÅpenBehandling(fagsak, åpenBehandling.get(), mottattDokument);
        } else {
            if (dokumentmottakerFelles.skalOppretteNyFørstegangsbehandling(fagsak)) {
                Optional<Behandling> behandlingOptional = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
                dokumentmottakerFelles.opprettNyFørstegangFraAvslag(mottattDokument, fagsak, behandlingOptional.get()); // NOSONAR
            } else {
                dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
            }
        }
    }

    private void håndterÅpenBehandling(Fagsak fagsak, Behandling behandling, MottattDokument mottattDokument) {
        if (behandling.erYtelseBehandling() && !kompletthetskontroller.vurderBehandlingKomplett(behandling).erOppfylt()) {
            kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        } else {
            dokumentmottakerFelles.opprettTaskForÅVurdereDokument(fagsak, behandling, mottattDokument);
        }
    }
}
