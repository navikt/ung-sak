package no.nav.k9.sak.behandling.prosessering.task;

import static no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask.TASKTYPE;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Kjører behandlingskontroll automatisk fra start.
 */
@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class StartBehandlingTask extends UnderBehandlingProsessTask {
    public static final String TASKTYPE = "behandlingskontroll.startBehandling";

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    protected StartBehandlingTask() {
    }

    @Inject
    public StartBehandlingTask(BehandlingRepository behandlingRepository, BehandlingLåsRepository behandlingLåsRepository, BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(behandlingRepository, behandlingLåsRepository);
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public void doProsesser(ProsessTaskData data, Behandling behandling) {
        precondition(behandling);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(data.getBehandlingId());
        behandlingskontrollTjeneste.prosesserBehandling(kontekst);
    }

    private void precondition(Behandling behandling) {
        var gyldigStatus = BehandlingStatus.OPPRETTET;
        if (!Objects.equals(gyldigStatus, behandling.getStatus())) {
            throw new IllegalStateException("Utvikler-feil: " + getClass().getSimpleName() + " kan kun benyttes på nyopprettet Behandling (med status " + gyldigStatus + ". Fikk: " + behandling);
        }
    }
}
