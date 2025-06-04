package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@ProsessTask(value = SettEtterlysningTilUtløptDersomVenterTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SettEtterlysningTilUtløptDersomVenterTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "etterlysning.planlagt.settUtlopt";
    public static final String ETTERLYSNING_ID = "etterlysningId";

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SettEtterlysningTilUtløptDersomVenterTask.class);

    private EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste;
    private BehandlingRepository behandlingRepository;
    private EtterlysningRepository etterlysningRepository;

    public SettEtterlysningTilUtløptDersomVenterTask() {
        // CDI
    }

    @Inject
    public SettEtterlysningTilUtløptDersomVenterTask(EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste, BehandlingRepository behandlingRepository, EtterlysningRepository etterlysningRepository) {
        this.etterlysningProssesseringTjeneste = etterlysningProssesseringTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.etterlysningRepository = etterlysningRepository;
    }

    @Override
    public Set<String> requiredProperties() {
        return Set.of(ETTERLYSNING_ID);
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erStatusFerdigbehandlet()) {
            LOG.info("Behandling var ferdigbehandlet, gjør ingenting");
            return;
        }
        var etterlysningId = Long.parseLong(prosessTaskData.getPropertyValue(ETTERLYSNING_ID));
        var etterlysning = etterlysningRepository.hentEtterlysning(etterlysningId);
        if (etterlysning.getStatus().equals(EtterlysningStatus.VENTER) && etterlysning.getFrist().isBefore(LocalDateTime.now())) {
            etterlysningProssesseringTjeneste.settEttelysningerUtløpt(List.of(etterlysning));
        } else {
            LOG.info("Etterlysning var ikke i VENTER status eller fristen var ikke utløpt, gjør ingenting");
        }
    }

}
