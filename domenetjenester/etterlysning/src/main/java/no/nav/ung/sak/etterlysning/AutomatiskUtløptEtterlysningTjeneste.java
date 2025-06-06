package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Dependent
public class AutomatiskUtløptEtterlysningTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(AutomatiskUtløptEtterlysningTjeneste.class);


    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    @Inject
    public AutomatiskUtløptEtterlysningTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void settEtterlysningerUtløpt() {

        List<Behandling> behandlingListe = behandlingKandidaterRepository.finnBehandlingerForUtløptEtterlysning();

        for (Behandling behandling : behandlingListe) {
            try {
                opprettProsessTask(behandling);
            } catch (Exception e) {
                logger.warn("Feil ved forsøk på sette etterlysninger til utløpt for behandling {}", behandling, e);
            }
        }
    }

    private void opprettProsessTask(Behandling behandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(SettEtterlysningerForBehandlingTilUtløptTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId());
        prosessTaskTjeneste.lagre(prosessTaskData);
    }
}
