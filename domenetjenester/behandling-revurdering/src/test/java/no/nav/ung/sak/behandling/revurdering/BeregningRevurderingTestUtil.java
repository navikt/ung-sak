package no.nav.ung.sak.behandling.revurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;

@ApplicationScoped
public class BeregningRevurderingTestUtil {

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    BeregningRevurderingTestUtil() {
        // for CDI
    }

    @Inject
    public BeregningRevurderingTestUtil(BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    public void avsluttBehandling(Behandling behandling) {
        if (behandling == null) {
            throw new IllegalStateException("Du må definere en behandling før du kan avslutten den");
        }
        avsluttBehandlingOgFagsak(behandling);
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }
}
