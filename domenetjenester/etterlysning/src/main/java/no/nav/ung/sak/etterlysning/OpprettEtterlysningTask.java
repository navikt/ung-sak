package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;

import java.util.Set;

@ApplicationScoped
@ProsessTask(value = OpprettEtterlysningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OpprettEtterlysningTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "etterlysning.opprett";
    public static final String ETTERLYSNING_TYPE = "type";
    private EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste;

    public OpprettEtterlysningTask() {
        // CDI
    }

    @Inject
    public OpprettEtterlysningTask(BehandlingRepository behandlingRepository,
                                   BehandlingLåsRepository behandlingLåsRepository,
                                   EtterlysningProssesseringTjeneste etterlysningProssesseringTjeneste) {
        super(behandlingRepository, behandlingLåsRepository);
        this.etterlysningProssesseringTjeneste = etterlysningProssesseringTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        final var etterlysningType = EtterlysningType.fraKode(prosessTaskData.getPropertyValue(ETTERLYSNING_TYPE));
        etterlysningProssesseringTjeneste.opprett(behandling, etterlysningType);
    }

    @Override
    public Set<String> requiredProperties() {
        return Set.of(ETTERLYSNING_TYPE);
    }


}
