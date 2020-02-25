package no.nav.foreldrepenger.økonomi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.foreldrepenger.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@SuppressWarnings("unused")
@ApplicationScoped
@ProsessTask(SendØkonomiOppdragTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendØkonomiOppdragTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.oppdragTilØkonomi";

    private K9OppdragRestKlient restKlient;
    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;

    SendØkonomiOppdragTask() {
        // for CDI proxy
    }

    @Inject
    public SendØkonomiOppdragTask(BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        TilkjentYtelseOppdrag input = tilkjentYtelseTjeneste.hentTilkjentYtelseOppdrag(behandlingId);
        restKlient.startIverksettelse(input);
    }

}
