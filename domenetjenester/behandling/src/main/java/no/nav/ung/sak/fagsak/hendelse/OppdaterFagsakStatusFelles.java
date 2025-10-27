package no.nav.ung.sak.fagsak.hendelse;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandling.FagsakStatusEventPubliserer;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;

@Dependent
public class OppdaterFagsakStatusFelles {

    private FagsakRepository fagsakRepository;
    private FagsakStatusEventPubliserer fagsakStatusEventPubliserer;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public OppdaterFagsakStatusFelles(BehandlingRepositoryProvider provider,
                                      FagsakStatusEventPubliserer fagsakStatusEventPubliserer) {
        this.fagsakRepository = provider.getFagsakRepository();
        this.fagsakStatusEventPubliserer = fagsakStatusEventPubliserer;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.behandlingVedtakRepository = provider.getBehandlingVedtakRepository();
    }

    public void oppdaterFagsakStatus(Fagsak fagsak, Behandling behandling, FagsakStatus nyStatus) {
        FagsakStatus gammelStatus = fagsak.getStatus();
        Long fagsakId = fagsak.getId();
        fagsakRepository.oppdaterFagsakStatus(fagsakId, nyStatus);

        if (fagsakStatusEventPubliserer != null) {
            fagsakStatusEventPubliserer.fireEvent(fagsak, behandling, gammelStatus, nyStatus);
        }
    }

    public boolean ingenLøpendeYtelsesvedtak(Behandling behandling) {
        Optional<Behandling> sisteInnvilgedeBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(behandling.getFagsakId());

        if (sisteInnvilgedeBehandling.isPresent()) {
            Behandling sisteBehandling = sisteInnvilgedeBehandling.get();
            var vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(sisteBehandling.getId());
            return erVedtakResultat(vedtak, VedtakResultatType.AVSLAG) || erVedtakResultat(vedtak, VedtakResultatType.OPPHØR);
        }
        return true;
    }

    private boolean erVedtakResultat(Optional<BehandlingVedtak> behandlingVedtakOpt, VedtakResultatType vedtakResultat) {
        return behandlingVedtakOpt
            .map(vedtak -> vedtak.getVedtakResultatType().equals(vedtakResultat))
            .orElse(Boolean.FALSE);
    }
}
