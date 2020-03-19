package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

public class RyddRegisterData {
    private final BehandlingRepository behandlingRepository;
    private final BehandlingskontrollKontekst kontekst;
    private MedlemskapRepository medlemskapRepository;

    public RyddRegisterData(BehandlingRepositoryProvider repositoryProvider, BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.kontekst = kontekst;
    }

    /**
     * @deprecated Erstatt med {@link #ryddRegisterdata()}
     */
    @Deprecated
    public void ryddRegisterdataLegacyEngangsstønad() {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        nullstillRegisterdata(behandling);
    }

    public void ryddRegisterdata() {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        nullstillRegisterdata(behandling);
    }

    private void nullstillRegisterdata(Behandling behandling) {
        medlemskapRepository.slettAvklarteMedlemskapsdata(behandling.getId(), kontekst.getSkriveLås());
        behandling.nullstillToTrinnsBehandling();
    }
}
