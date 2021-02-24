package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import java.util.Objects;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

public class RyddVilkårTyper {

    private final Behandling behandling;
    private final BehandlingskontrollKontekst kontekst;
    private BehandlingRepository behandlingRepository;

    public RyddVilkårTyper(@SuppressWarnings("unused") BehandlingStegModell modell,
                           BehandlingRepositoryProvider repositoryProvider,
                           Behandling behandling,
                           BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandling = behandling;
        this.kontekst = kontekst;
    }

    public RyddVilkårTyper(@SuppressWarnings("unused") BehandlingStegModell modell,
                           BehandlingRepository behandlingRepository,
                           Behandling behandling,
                           BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = behandlingRepository;
        this.behandling = behandling;
        this.kontekst = kontekst;
    }

    public void ryddVedTilbakeføring() {
        nullstillVedtaksresultat();
    }

    private void nullstillVedtaksresultat() {
        if (Objects.equals(behandling.getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }

        behandling.setBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

}
