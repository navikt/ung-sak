package no.nav.foreldrepenger.fagsak.hendelse;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;

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
        Optional<Behandling> sisteInnvilgedeBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());

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
