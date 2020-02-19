package no.nav.foreldrepenger.økonomi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.foreldrepenger.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.InntrekkBeslutning;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelse;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseBehandlingInfoV1;
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
    private BehandlingRepository behandlingRepository;

    SendØkonomiOppdragTask() {
        // for CDI proxy
    }

    @Inject
    public SendØkonomiOppdragTask(BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        Behandling behandling = behandlingRepository.finnUnikBehandlingForBehandlingId(behandlingId).orElseThrow();
        TilkjentYtelse tilkjentYtelse = tilkjentYtelseTjeneste.hentilkjentYtelse(behandlingId);
        TilkjentYtelseBehandlingInfoV1 behandlingInfo = tilkjentYtelseTjeneste.hentilkjentYtelseBehandlingInfo(behandlingId);
        //FIXME K9 inntil simulering er implementert er inntrekkbeslutning alltid 'bruk inntrekk'
        InntrekkBeslutning inntrekkBeslutning = new InntrekkBeslutning(true);
        TilkjentYtelseOppdrag input = new TilkjentYtelseOppdrag(tilkjentYtelse, behandlingInfo, behandling.getUuid(), inntrekkBeslutning);
        restKlient.startIverksettelse(input);
    }

}
