package no.nav.k9.sak.hendelse.vedtak;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;


@ApplicationScoped
@ActivateRequestContext
@Transactional
public class VedtaksHendelseHåndterer {

    private static final Logger log = LoggerFactory.getLogger(VedtaksHendelseHåndterer.class);
    private ProsessTaskRepository taskRepository;

    VedtaksHendelseHåndterer() {
    }

    @Inject
    public VedtaksHendelseHåndterer(ProsessTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    void handleMessage(String key, String payload) {
        log.debug("Mottatt ytelse-vedtatt hendelse med key='{}', payload={}", key, payload);
        var vh = JsonObjectMapper.fromJson(payload, Ytelse.class);
        var fagsakYtelseType = mapYtelse(vh);

        var vurderOmVedtakPåvirkerSakerTjeneste = VurderOmVedtakPåvirkerSakerTjeneste.finnTjenesteHvisStøttet(fagsakYtelseType);
        if (vurderOmVedtakPåvirkerSakerTjeneste.isEmpty()) {
            return;
        }
        log.info("Mottatt ytelse-vedtatt hendelse med ytelse='{}' saksnummer='{}', sjekker behovet for revurdering", fagsakYtelseType, vh.getSaksnummer());

        ProsessTaskData taskData = new ProsessTaskData(VurderOmVedtakPåvirkerAndreSakerTask.TASKNAME);
        taskData.setPayload(payload);

        taskRepository.lagre(taskData);
    }

    static FagsakYtelseType mapYtelse(Ytelse vedtak) {
        if (vedtak.getYtelse() == null) {
            return Optional.ofNullable(vedtak.getType()).map(YtelseType::getKode)
                .map(FagsakYtelseType::fromString).orElse(FagsakYtelseType.UDEFINERT);
        }
        return switch (vedtak.getYtelse()) {
            case PLEIEPENGER_NÆRSTÅENDE -> FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case PLEIEPENGER_SYKT_BARN -> FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
            case OMSORGSPENGER -> FagsakYtelseType.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> FagsakYtelseType.OPPLÆRINGSPENGER;
            case FRISINN -> FagsakYtelseType.FRISINN;
            case ENGANGSTØNAD -> FagsakYtelseType.ENGANGSTØNAD;
            case FORELDREPENGER -> FagsakYtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> FagsakYtelseType.SVANGERSKAPSPENGER;
        };
    }
}
