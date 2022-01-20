package no.nav.k9.sak.domene.registerinnhenting.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;

@ApplicationScoped
@ProsessTask(DiffOgReposisjonerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class DiffOgReposisjonerTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "grunnlag.diffOgReposisjoner";
    public static final String UTLED_ÅRSAKER = "skalUtledeÅrsaker";

    private RegisterdataEndringshåndterer endringshåndterer;

    DiffOgReposisjonerTask() {
        // for CDI proxy
    }

    @Inject
    public DiffOgReposisjonerTask(BehandlingRepository repository, BehandlingLåsRepository behandlingLåsRepository, RegisterdataEndringshåndterer endringshåndterer) {
        super(repository, behandlingLåsRepository);
        this.endringshåndterer = endringshåndterer;
    }

    @Override
    public void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        var propertyValue = prosessTaskData.getPropertyValue(UTLED_ÅRSAKER);
        var skalutledeÅrsaker = true;

        if (propertyValue != null && !propertyValue.isEmpty()) {
            skalutledeÅrsaker = Boolean.parseBoolean(propertyValue);
        }

        endringshåndterer.utledDiffOgReposisjonerBehandlingVedEndringer(behandling, hentUtSnapshotFraPayload(prosessTaskData), skalutledeÅrsaker);
    }

    EndringsresultatSnapshot hentUtSnapshotFraPayload(ProsessTaskData prosessTaskData) {
        var payloadAsString = prosessTaskData.getPayloadAsString();
        if (payloadAsString == null) {
            return EndringsresultatSnapshot.opprett();
        }
        return JsonObjectMapper.fromJson(payloadAsString, EndringsresultatSnapshot.class);
    }

}
