package no.nav.ung.sak.behandling.prosessering.task;

import static no.nav.ung.sak.behandling.prosessering.task.StartBehandlingTask.TASKTYPE;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

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
        var gyldigStatus = Set.of(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES);
        if (!gyldigStatus.contains(behandling.getStatus())) {
            throw new IllegalStateException("Utvikler-feil: " + getClass().getSimpleName() + " kan kun benyttes på nyopprettet Behandling (med status " + gyldigStatus + ". Fikk: " + behandling);
        }

        // TODO sjekk på steg? denne vil vel ikke gå forbi kompletthet eller inreg?
    }
}
