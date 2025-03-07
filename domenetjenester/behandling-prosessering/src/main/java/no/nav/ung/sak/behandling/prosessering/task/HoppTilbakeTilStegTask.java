package no.nav.ung.sak.behandling.prosessering.task;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;

@ApplicationScoped
@ProsessTask(HoppTilbakeTilStegTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class HoppTilbakeTilStegTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.hoppTilbake";

    public static final String PROPERTY_TIL_STEG = "tilSteg";

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    HoppTilbakeTilStegTask() {
        // CDI
    }

    @Inject
    public HoppTilbakeTilStegTask(BehandlingRepository repository, BehandlingLåsRepository behandlingLåsRepository, BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(repository, behandlingLåsRepository);
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
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
