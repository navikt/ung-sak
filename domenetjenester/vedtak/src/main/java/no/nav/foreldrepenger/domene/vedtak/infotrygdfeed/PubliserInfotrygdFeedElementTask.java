package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.kafka.InfotrygdFeedMeldingProducer;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(PubliserInfotrygdFeedElementTask.TASKTYPE)
public class PubliserInfotrygdFeedElementTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.publiserInfotrygdFeedElement";
    public static final String KAFKA_KEY_PROPERTY = TASKTYPE + ".kafkaKey";

    private static final Logger logger = LoggerFactory.getLogger(PubliserInfotrygdFeedElementTask.class);

    private BehandlingRepository behandlingRepository;
    private InfotrygdFeedMeldingProducer meldingProducer;
    private Instance<InfotrygdFeedPeriodeberegner> periodeBeregnere;

    public PubliserInfotrygdFeedElementTask() {
        // CDI
    }

    @Inject
    public PubliserInfotrygdFeedElementTask(BehandlingRepository behandlingRepository,
                                            InfotrygdFeedMeldingProducer meldingProducer,
                                            @Any Instance<InfotrygdFeedPeriodeberegner> periodeBeregnere) {
        this.behandlingRepository = behandlingRepository;
        this.meldingProducer = meldingProducer;
        this.periodeBeregnere = periodeBeregnere;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        var behandling = behandlingRepository.hentBehandling(pd.getBehandlingId());

        String key = pd.getPropertyValue(KAFKA_KEY_PROPERTY);
        String value = getInfotrygdFeedMessage(behandling).toJson();

        logger.info("Publiserer hendelse til Infotrygd Feed. Key: '{}'", key);

        meldingProducer.send(key, value);
    }

    InfotrygdFeedMessage getInfotrygdFeedMessage(Behandling behandling) {
        InfotrygdFeedMessage.Builder builder = InfotrygdFeedMessage.builder()
            .uuid(UUID.randomUUID().toString());

        setSaksnummerOgAktørId(builder, behandling.getFagsak());
        setPeriodeOgYtelse(builder, behandling);
        setAktørIdPleietrengende(builder, behandling);

        return builder.build();
    }

    private void setSaksnummerOgAktørId(InfotrygdFeedMessage.Builder builder, Fagsak fagsak) {
        builder
            .saksnummer(fagsak.getSaksnummer().getVerdi())
            .aktoerId(fagsak.getAktørId().getId());
    }

    private void setPeriodeOgYtelse(InfotrygdFeedMessage.Builder builder, Behandling behandling) {
        InfotrygdFeedPeriodeberegner periodeBeregner = getInfotrygdFeedPeriodeBeregner(behandling);
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        builder.ytelse(periodeBeregner.getInfotrygdYtelseKode(saksnummer));

        InfotrygdFeedPeriode periode = periodeBeregner.finnInnvilgetPeriode(saksnummer);

        LocalDate fom = periode.getFom();
        LocalDate tom = periode.getTom();

        if (!Objects.equals(Tid.TIDENES_BEGYNNELSE, fom)) {
            builder.foersteStoenadsdag(fom);
        }
        if (!Objects.equals(Tid.TIDENES_ENDE, tom)) {
            builder.sisteStoenadsdag(tom);
        }
    }

    private void setAktørIdPleietrengende(InfotrygdFeedMessage.Builder builder, Behandling behandling) {
        AktørId pleietrengendeAktørId = behandling.getFagsak().getPleietrengendeAktørId();
        if (pleietrengendeAktørId != null) {
            builder.aktoerIdPleietrengende(pleietrengendeAktørId.getId());
        }
    }

    private InfotrygdFeedPeriodeberegner getInfotrygdFeedPeriodeBeregner(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsak().getYtelseType();

        return FagsakYtelseTypeRef.Lookup.find(periodeBeregnere, ytelseType)
            .orElseThrow(() -> new IllegalArgumentException("Kan ikke beregne periode for ytelse: " + ytelseType));
    }
}
