package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.task.ÅrskvantumDeaktiveringTask;

@Dependent
public class ÅrskvantumDeaktiveringTjeneste {

    public Optional<ProsessTaskData> meldFraDersomDeaktivering(Behandling behandling) {
        if (ÅrskvantumDeaktiveringTask.skalDeaktivere(behandling)) {
            ProsessTaskData prosessTaskData =  ProsessTaskData.forProsessTask(ÅrskvantumDeaktiveringTask.class);

            prosessTaskData.setBehandling(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getId().toString(), behandling.getAktørId().getId());
            return Optional.of(prosessTaskData);
        }
        return Optional.empty();
    }
}
