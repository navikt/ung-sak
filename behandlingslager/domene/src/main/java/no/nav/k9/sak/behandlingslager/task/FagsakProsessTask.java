package no.nav.k9.sak.behandlingslager.task;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLåsRepository;

/**
 * Task som utfører noe på en fagsak, før prosessen kjøres videre.
 * Sikrer at fagsaklås task på riktig plass..
 */
public abstract class FagsakProsessTask implements ProsessTaskHandler {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");

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
        Long fagsakId = Objects.requireNonNull(prosessTaskData.getFagsakId(), "fagsakId");

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

    /** log mdc cleares automatisk når task har kjørt, så trenger ikke kalle clearLogContext. */
    public static void logContext(Fagsak fagsak) {
        LOG_CONTEXT.add("saksnummer", fagsak.getSaksnummer());
        LOG_CONTEXT.add("ytelseType", fagsak.getYtelseType());
    }

    /** symmetri til #logContext. */
    public static void clearLogContext() {
        LOG_CONTEXT.remove("saksnummer");
        LOG_CONTEXT.remove("ytelseType");
    }

}
