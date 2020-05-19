package no.nav.k9.sak.behandlingslager.task;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Task som utfører noe på en fagsak, før prosessen kjøres videre.
 * Sikrer at fagsaklås task på riktig plass..
 */
public abstract class FagsakProsessTask implements ProsessTaskHandler {

    private FagsakLåsRepository fagsakLåsRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    protected FagsakProsessTask(FagsakLåsRepository fagsakLåsRepository, BehandlingLåsRepository behandlingLåsRepository) {
        this.fagsakLåsRepository = fagsakLåsRepository;
        this.behandlingLåsRepository = behandlingLåsRepository;
    }

    protected FagsakProsessTask() {
        // for CDI proxy
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long fagsakId = prosessTaskData.getFagsakId();

        identifiserBehandling(prosessTaskData)
            .stream()
            .sorted(Comparator.naturalOrder())
            .forEach(behandlingId -> behandlingLåsRepository.taLås(behandlingId));

        fagsakLåsRepository.taLås(fagsakId);

            prosesser(prosessTaskData);
    }

    protected abstract void prosesser(ProsessTaskData prosessTaskData);

    /**
     * Må alltid ta behandlingen før vi tar lås på fagsaken.
     * Ellers risikerer vi deadlock.
     * <p>
     * Identifiserer behandlingen som skal manipuleres
     *
     * @param prosessTaskData prosesstaskdata
     * @return behandlingId
     */
    protected List<String> identifiserBehandling(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        if (behandlingId != null) {
            return List.of(behandlingId);
        }
        return Collections.emptyList();
    }
}
