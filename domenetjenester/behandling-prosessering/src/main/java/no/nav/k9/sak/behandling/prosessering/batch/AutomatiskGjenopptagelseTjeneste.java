package no.nav.k9.sak.behandling.prosessering.batch;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class AutomatiskGjenopptagelseTjeneste {

    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;

    @Inject
    public AutomatiskGjenopptagelseTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                            BehandlingProsesseringTjeneste prosesseringTjeneste,
                                            ProsessTaskRepository prosessTaskRepository) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.prosesseringTjeneste = prosesseringTjeneste;
    }

    public AutomatiskGjenopptagelseTjeneste() {
        // for CDI
    }

    public void gjenopptaBehandlinger() {
        List<Behandling> behandlingListe = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        for (Behandling behandling : behandlingListe) {
            opprettProsessTask(behandling);
        }
    }

    private void opprettProsessTask(Behandling behandling) {
        prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, true);
    }
}
