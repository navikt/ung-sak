package no.nav.foreldrepenger.økonomi;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

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
    public SendØkonomiOppdragTask(BehandlingRepositoryProvider repositoryProvider, K9OppdragRestKlient restKlient, TilkjentYtelseTjeneste tilkjentYtelseTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.restKlient = restKlient;
        this.tilkjentYtelseTjeneste = tilkjentYtelseTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        TilkjentYtelseOppdrag input = tilkjentYtelseTjeneste.hentTilkjentYtelseOppdrag(behandlingId);
        input.getBehandlingsinfo().setBehandlingTidspunkt(hentOpprinneligIverksettelseTidspunkt(prosessTaskData));
        restKlient.startIverksettelse(input);
    }

    private OffsetDateTime hentOpprinneligIverksettelseTidspunkt(ProsessTaskData prosessTaskData) {
        String tidspunkt = prosessTaskData.getPropertyValue("opprinneligIversettingTidspunkt");
        if (tidspunkt == null) {
            throw new IllegalArgumentException("Mangler verdi for opprinneligIversettingTidspunkt");
        }
        return OffsetDateTime.parse(tidspunkt, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

}
