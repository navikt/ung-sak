package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.task.ÅrskvantumDeaktiveringTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class ÅrskvantumDeaktiveringTjenesteImpl {

    private ProsessTaskRepository prosessTaskRepository;

    ÅrskvantumDeaktiveringTjenesteImpl() {
        // CDI
    }

    @Inject
    public ÅrskvantumDeaktiveringTjenesteImpl(ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void meldIfraOmIverksetting(Behandling behandling) {
        if (ÅrskvantumDeaktiveringTask.skalDeaktivere(behandling)) {
            ProsessTaskData prosessTaskData = new ProsessTaskData(ÅrskvantumDeaktiveringTask.TASKTYPE);

            prosessTaskData.setBehandling(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getId().toString(), behandling.getAktørId().getId());
            prosessTaskRepository.lagre(prosessTaskData);
        }
    }
}
