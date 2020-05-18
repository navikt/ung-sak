package no.nav.k9.sak.domene.registerinnhenting.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(DiffOgReposisjonerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class DiffOgReposisjonerTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "grunnlag.diffOgReposisjoner";

    private BehandlingRepository repository;
    private RegisterdataEndringshåndterer endringshåndterer;

    DiffOgReposisjonerTask() {
        // for CDI proxy
    }

    @Inject
    public DiffOgReposisjonerTask(BehandlingRepository repository, RegisterdataEndringshåndterer endringshåndterer) {
        this.repository = repository;
        this.endringshåndterer = endringshåndterer;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandling = repository.hentBehandling(prosessTaskData.getBehandlingId());
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
