package no.nav.ung.sak.behandling.prosessering.gjenopptagelse;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingKandidaterRepository;

@ApplicationScoped
public class AutomatiskGjenopptagelseTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(AutomatiskGjenopptagelseTjeneste.class);

    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;

    @Inject
    public AutomatiskGjenopptagelseTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                            BehandlingProsesseringTjeneste prosesseringTjeneste) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.prosesseringTjeneste = prosesseringTjeneste;
    }

    public AutomatiskGjenopptagelseTjeneste() {
        // for CDI
    }

    public void gjenopptaBehandlinger() {
        List<Behandling> behandlingListe = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        for (Behandling behandling : behandlingListe) {
            try {
                opprettProsessTask(behandling);
            } catch (Exception e) {
                logger.warn("Feil ved forsøk på gjennoppta for behandling {}", behandling, e);
            }
        }
    }

    private void opprettProsessTask(Behandling behandling) {
        prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, true);
    }
}
