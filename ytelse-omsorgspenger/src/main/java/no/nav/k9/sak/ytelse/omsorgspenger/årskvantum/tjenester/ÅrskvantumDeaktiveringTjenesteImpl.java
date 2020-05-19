package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.vedtak.årskvantum.ÅrskvantumDeaktiveringTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.task.ÅrskvantumDeaktiveringTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;


@ApplicationScoped
public class ÅrskvantumDeaktiveringTjenesteImpl implements ÅrskvantumDeaktiveringTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ÅrskvantumDeaktiveringTjenesteImpl.class);

    private ProsessTaskRepository prosessTaskRepository;

    ÅrskvantumDeaktiveringTjenesteImpl() {
        // CDI
    }

    @Inject
    public ÅrskvantumDeaktiveringTjenesteImpl(ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void meldIfraOmIverksetting(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(ÅrskvantumDeaktiveringTask.TASKTYPE);

        prosessTaskData.setBehandling(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getId().toString(),behandling.getAktørId().getId());
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
