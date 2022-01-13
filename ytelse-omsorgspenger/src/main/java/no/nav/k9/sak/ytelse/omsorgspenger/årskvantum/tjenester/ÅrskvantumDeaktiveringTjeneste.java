package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.task.ÅrskvantumDeaktiveringTask;

@Dependent
public class ÅrskvantumDeaktiveringTjeneste {

    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public ÅrskvantumDeaktiveringTjeneste(ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void meldFraDersomDeaktivering(Behandling behandling) {
        if (ÅrskvantumDeaktiveringTask.skalDeaktivere(behandling)) {
            ProsessTaskData prosessTaskData = new ProsessTaskData(ÅrskvantumDeaktiveringTask.TASKTYPE);

            prosessTaskData.setBehandling(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getId().toString(), behandling.getAktørId().getId());
            prosessTaskRepository.lagre(prosessTaskData);
        }
    }
}
