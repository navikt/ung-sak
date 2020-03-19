package no.nav.k9.sak.økonomi.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(SendTilkjentYtelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendTilkjentYtelseTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.sendTilkjentYtelse";
    private TilkjentYtelseMeldingProducer meldingProducer;
    private BehandlingRepository behandlingRepository;

    public SendTilkjentYtelseTask() {
        // CDI krav
    }

    @Inject
    public SendTilkjentYtelseTask(TilkjentYtelseMeldingProducer meldingProducer, BehandlingRepository behandlingRepository) {
        this.meldingProducer = meldingProducer;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        meldingProducer.sendTilkjentYtelse(behandling);
    }

}
