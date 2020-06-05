package no.nav.k9.sak.behandlingslager.task;

import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.log.mdc.MdcExtendedLogContext;

/** Håndterer preconditions for å kunne innhente + ekstra log parametere.  Tillater ikke kjøring dersom saksbehandling er avsluttet (utvikler-feil). */
public abstract class UnderBehandlingProsessTask extends BehandlingProsessTask {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");

    private BehandlingRepository repository;

    protected UnderBehandlingProsessTask() {
        // for CDI proxy
    }

    @Inject
    protected UnderBehandlingProsessTask(BehandlingRepository repository, BehandlingLåsRepository behandlingLåsRepository) {
        super(behandlingLåsRepository);
        this.repository = repository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandling = repository.hentBehandling(prosessTaskData.getBehandlingId());

        logContext(behandling);
        
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil - saken er ferdig behandlet, kan ikke oppdateres. behandlingId=" + behandling.getId()
                + ", behandlingStatus=" + behandling.getStatus()
                + ", startpunkt=" + behandling.getStartpunkt()
                + ", resultat=" + behandling.getBehandlingResultatType());
        } else {
            doProsesser(prosessTaskData);
        }
    }

    protected abstract void doProsesser(ProsessTaskData prosessTaskData);

    private void logContext(Behandling behandling) {
        LOG_CONTEXT.add("saksnummer", behandling.getFagsak().getSaksnummer());
        LOG_CONTEXT.add("ytelseType", behandling.getFagsakYtelseType());
        LOG_CONTEXT.add("behandling_status", behandling.getStatus());
        behandling.getBehandlingStegTilstand().ifPresent(st -> LOG_CONTEXT.add("steg", st.getBehandlingSteg()));
        behandling.getBehandlingStegTilstand().ifPresent(st -> LOG_CONTEXT.add("steg_status", st.getBehandlingStegStatus()));
    }

}
