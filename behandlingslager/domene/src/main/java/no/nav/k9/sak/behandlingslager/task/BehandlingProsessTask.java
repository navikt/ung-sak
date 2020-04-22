package no.nav.k9.sak.behandlingslager.task;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.log.mdc.MdcExtendedLogContext;

/**
 * Task som utfører noe på en behandling, før prosessen kjøres videre.
 * Sikrer at behandlingslås task på riktig plass.
 * Tasks som forsøker å kjøre behandling videre bør extende denne.
 */
public abstract class BehandlingProsessTask implements ProsessTaskHandler {
    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$

    private BehandlingLåsRepository behandlingLåsRepository;

    protected BehandlingProsessTask(BehandlingLåsRepository BehandlingLåsRepository) {
        this.behandlingLåsRepository = BehandlingLåsRepository;
    }

    protected BehandlingProsessTask() {
        // for CDI proxy
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        behandlingLåsRepository.taLås(behandlingId);

        try {
            prosesser(prosessTaskData);
        } finally {
            LOG_CONTEXT.clear();
        }
    }

    protected abstract void prosesser(ProsessTaskData prosessTaskData);

}
