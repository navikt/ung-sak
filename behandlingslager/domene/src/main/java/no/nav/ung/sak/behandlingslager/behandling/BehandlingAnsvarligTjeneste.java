package no.nav.ung.sak.behandlingslager.behandling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.BehandlingStegDel;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;

import java.util.Optional;

@Dependent
public class BehandlingAnsvarligTjeneste {

    private final BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    @Inject
    public BehandlingAnsvarligTjeneste(BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    public Optional<BehandlingAnsvarlig> finnBehandlingAnsvarligFraBehandlingTilstand(Behandling behandling) {
        BehandlingDel aktivDel = BehandlingStegDel.utled(behandling.getFagsakYtelseType(), behandling.getAktivtBehandlingSteg())
            .orElse(BehandlingDel.SENTRAL);
        return behandlingAnsvarligRepository.hentBehandlingAnsvarlig(behandling.getId(), aktivDel);
    }
}
