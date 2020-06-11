package no.nav.k9.sak.domene.registerinnhenting.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(DiffOgReposisjonerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class DiffOgReposisjonerTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "grunnlag.diffOgReposisjoner";

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
        endringshåndterer.utledDiffOgReposisjonerBehandlingVedEndringer(behandling, hentUtSnapshotFraPayload(prosessTaskData));
    }

    EndringsresultatSnapshot hentUtSnapshotFraPayload(ProsessTaskData prosessTaskData) {
        var payloadAsString = prosessTaskData.getPayloadAsString();
        if (payloadAsString == null) {
            return EndringsresultatSnapshot.opprett();
        }
        return JsonObjectMapper.fromJson(payloadAsString, EndringsresultatSnapshot.class);
    }

}
