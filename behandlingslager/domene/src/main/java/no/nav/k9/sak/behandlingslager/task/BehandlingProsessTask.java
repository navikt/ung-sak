package no.nav.k9.sak.behandlingslager.task;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
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

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");

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

        if (behandlingLåsRepository != null) {
            behandlingLåsRepository.taLås(behandlingId);
        }

        prosesser(prosessTaskData);
    }

    protected abstract void prosesser(ProsessTaskData prosessTaskData);

    public static void logContext(Behandling behandling) {
        LOG_CONTEXT.add("saksnummer", behandling.getFagsak().getSaksnummer());
        LOG_CONTEXT.add("ytelseType", behandling.getFagsakYtelseType());
        LOG_CONTEXT.add("behandling_status", behandling.getStatus());
        behandling.getBehandlingStegTilstand().ifPresent(st -> LOG_CONTEXT.add("steg", st.getBehandlingSteg()));
        behandling.getBehandlingStegTilstand().ifPresent(st -> LOG_CONTEXT.add("steg_status", st.getBehandlingStegStatus()));
    }

    /** symmetri til #logContext. */
    public static void clearLogContext() {
        LOG_CONTEXT.remove("saksnummer");
        LOG_CONTEXT.remove("ytelseType");
        LOG_CONTEXT.remove("behandling_status");
        LOG_CONTEXT.remove("steg");
        LOG_CONTEXT.remove("steg_status");
    }

}
