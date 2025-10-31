package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Kjører behandlingskontroll automatisk fra der prosessen står.
 */
@ApplicationScoped
@ProsessTask(FortsettBehandlingDersomIkkePåVentTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class FortsettBehandlingDersomIkkePåVentTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.sjekkVentOgFortsettBehandling";
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(FortsettBehandlingDersomIkkePåVentTask.class);
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    FortsettBehandlingDersomIkkePåVentTask() {
        // For CDI proxy
    }

    @Inject
    public FortsettBehandlingDersomIkkePåVentTask(BehandlingRepository behandlingRepository,
                                                  BehandlingLåsRepository behandlingLåsRepository,
                                                  BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(behandlingRepository, behandlingLåsRepository);
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public void doProsesser(ProsessTaskData data, Behandling behandling) {
        var behandlingId = data.getBehandlingId();
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);

        Boolean manuellFortsettelse = Optional.ofNullable(data.getPropertyValue(FortsettBehandlingTask.MANUELL_FORTSETTELSE))
            .map(Boolean::valueOf)
            .orElse(Boolean.FALSE);

        if (manuellFortsettelse) {
            if (behandling.isBehandlingPåVent()) { // Autopunkt
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
            }
        }
        if (behandling.isBehandlingPåVent()) {
            logger.info("Behandling {} er på vent, fortsetter ikke behandling nå.", behandlingId);
            return;
        }
        if (behandling.erAvsluttet()) {
            throw new IllegalStateException("Kan ikke fortsette en avsluttet behandling");
        }
        behandlingskontrollTjeneste.prosesserBehandling(kontekst);

    }
}
