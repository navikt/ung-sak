package no.nav.k9.sak.behandlingslager.task;

import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/** Håndterer preconditions for å kunne innhente + ekstra log parametere.  Tillater ikke kjøring dersom saksbehandling er avsluttet (utvikler-feil). */
public abstract class UnderBehandlingProsessTask extends BehandlingProsessTask {

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
        
        if (behandling.erAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil - saken er ferdig behandlet, kan ikke oppdateres. behandlingId=" + behandling.getId()
                + ", behandlingStatus=" + behandling.getStatus()
                + ", startpunkt=" + behandling.getStartpunkt()
                + ", resultat=" + behandling.getBehandlingResultatType());
        } else {
            doProsesser(prosessTaskData, behandling);
        }
    }

    protected abstract void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling);

}
