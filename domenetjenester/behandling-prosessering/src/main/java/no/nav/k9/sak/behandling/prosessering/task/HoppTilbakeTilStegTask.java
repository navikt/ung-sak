package no.nav.k9.sak.behandling.prosessering.task;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;

@ApplicationScoped
@ProsessTask(HoppTilbakeTilStegTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class HoppTilbakeTilStegTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.hoppTilbake";

    public static final String PROPERTY_TIL_STEG = "tilSteg";

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    HoppTilbakeTilStegTask() {
        // CDI
    }

    @Inject
    public HoppTilbakeTilStegTask(BehandlingRepository repository, BehandlingLåsRepository behandlingLåsRepository, BehandlingskontrollTjeneste behandlingskontrollTjeneste, ProsesseringAsynkTjeneste prosesseringAsynkTjeneste, FagsakProsessTaskRepository prosessTaskRepository) {
        super(repository, behandlingLåsRepository);
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        var tilSteg = Objects.requireNonNull(prosessTaskData.getPropertyValue(PROPERTY_TIL_STEG));

        var behandlingStegType = BehandlingStegType.fraKode(tilSteg);

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

        hoppTilbake(behandling, behandlingStegType, kontekst);
    }


    private void hoppTilbake(Behandling behandling, BehandlingStegType tilSteg, BehandlingskontrollKontekst kontekst) {
        doHoppTilSteg(behandling, kontekst, tilSteg);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        // Fortsett behandling tasken er allerede planlagt Og trenger ikke planlegge en ny
    }

    private void doHoppTilSteg(Behandling behandling, BehandlingskontrollKontekst kontekst, BehandlingStegType tilSteg) {
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);

        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tilSteg);
    }
}
