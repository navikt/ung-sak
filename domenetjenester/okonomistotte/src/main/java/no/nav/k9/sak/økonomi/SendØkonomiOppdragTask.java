package no.nav.k9.sak.økonomi;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.oppdrag.kontrakt.util.TilkjentYtelseMaskerer;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.domene.uttak.rest.JsonMapper;
import no.nav.k9.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.k9.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
@ProsessTask(SendØkonomiOppdragTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendØkonomiOppdragTask extends BehandlingProsessTask {

    public static final Logger logger = LoggerFactory.getLogger(SendØkonomiOppdragTask.class);
    public static final String TASKTYPE = "iverksetteVedtak.oppdragTilØkonomi";

    private ObjectMapper objectMapper = JsonMapper.getMapper();
    private TilkjentYtelseMaskerer maskerer = new TilkjentYtelseMaskerer(objectMapper).ikkeMaskerSats();

    private K9OppdragRestKlient restKlient;
    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;

    SendØkonomiOppdragTask() {
        // for CDI proxy
    }

    @Inject
    public SendØkonomiOppdragTask(BehandlingRepositoryProvider repositoryProvider,
                                  K9OppdragRestKlient restKlient,
                                  TilkjentYtelseTjeneste tilkjentYtelseTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.restKlient = restKlient;
        this.tilkjentYtelseTjeneste = tilkjentYtelseTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        TilkjentYtelseOppdrag input = tilkjentYtelseTjeneste.hentTilkjentYtelseOppdrag(behandlingId);
        input.getBehandlingsinfo().setBehandlingTidspunkt(hentOpprinneligIverksettelseTidspunkt(prosessTaskData));

        logger.info("Sender {} perioder med tilkjent ytelse for behandlingId={}", input.getTilkjentYtelse().getPerioder().size(), input.getBehandlingId());

        // FIXME K9 Midlertidig kode for å feilsøke integrasjonen mot k9-oppdrag
        if (!Environment.current().isProd() && logger.isInfoEnabled()) {
            try {
                TilkjentYtelseOppdrag maskert = maskerer.masker(input);
                logger.info("Sender til k9-oppdrag (maskert): {}", objectMapper.writeValueAsString(maskert));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Fikk Json-feil", e);
            }
        }

        restKlient.startIverksettelse(input);
    }

    private OffsetDateTime hentOpprinneligIverksettelseTidspunkt(ProsessTaskData prosessTaskData) {
        String tidspunkt = prosessTaskData.getPropertyValue("opprinneligIverksettingTidspunkt");
        if (tidspunkt == null) {
            throw new IllegalArgumentException("Mangler verdi for opprinneligIverksettingTidspunkt: " + prosessTaskData);
        }
        return OffsetDateTime.parse(tidspunkt, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

}
