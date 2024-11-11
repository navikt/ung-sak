package no.nav.ung.sak.produksjonsstyring.behandlingenhet;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(OppdaterBehandlendeEnhetTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OppdaterBehandlendeEnhetTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "oppgavebehandling.oppdaterEnhet";

    public static final String BEGRUNNELSE = "Enhetsomlegging";

    private static final Logger log = LoggerFactory.getLogger(OppdaterBehandlendeEnhetTask.class);

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    OppdaterBehandlendeEnhetTask() {
        // for CDI proxy
    }

    @Inject
    public OppdaterBehandlendeEnhetTask(BehandlingRepositoryProvider repositoryProvider, BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        super(repositoryProvider.getBehandlingRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        Optional<OrganisasjonsEnhet> nyEnhet = behandlendeEnhetTjeneste.sjekkOppdatertEnhetEtterReallokering(behandling);
        if (nyEnhet.isPresent()) {
            log.info("Endrer behandlende enhet for behandling: {}", prosessTaskData.getBehandlingId()); // NOSONAR
            behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, nyEnhet.get(), HistorikkAktør.VEDTAKSLØSNINGEN, BEGRUNNELSE);
        }
    }
}
