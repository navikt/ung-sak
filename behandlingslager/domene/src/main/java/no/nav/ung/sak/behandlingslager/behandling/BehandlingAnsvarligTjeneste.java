package no.nav.ung.sak.behandlingslager.behandling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.BehandlingStegDel;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;

import java.util.Map;
import java.util.Optional;

@Dependent
public class BehandlingAnsvarligTjeneste {

    private final BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    @Inject
    public BehandlingAnsvarligTjeneste(BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    public Optional<BehandlingAnsvarlig> finnBehandlingAnsvarligFraBehandlingTilstand(Behandling behandling) {
        Map<BehandlingDel, BehandlingAnsvarlig> ansvarlige = behandlingAnsvarligRepository.hentBehandlingAnsvarlige(behandling.getId());

        if (ansvarlige.isEmpty()) {
            return Optional.empty();
        }

        if (behandling.getFagsakYtelseType() == FagsakYtelseType.UNGDOMSYTELSE) {
            return Optional.of(ansvarlige.get(BehandlingDel.SENTRAL));
        }

        var aktivSteg = behandling.getAktivtBehandlingSteg();
        BehandlingDel aktivDel = BehandlingStegDel.forAktivitetspenger(aktivSteg);
        return Optional.of(ansvarlige.get(aktivDel));

    }
}
