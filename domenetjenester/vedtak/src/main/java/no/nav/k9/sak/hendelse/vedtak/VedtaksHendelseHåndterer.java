package no.nav.k9.sak.hendelse.vedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        var fagsakYtelseType = FagsakYtelseType.fromString(vh.getType().getKode());

        var vurderOmVedtakPåvirkerSakerTjeneste = VurderOmVedtakPåvirkerSakerTjeneste.finnTjenesteHvisStøttet(fagsakYtelseType);
        if (vurderOmVedtakPåvirkerSakerTjeneste.isEmpty()) {
            return;
        }
        log.info("Mottatt ytelse-vedtatt hendelse med ytelse='{}' saksnummer='{}', sjekker behovet for revurdering", fagsakYtelseType, vh.getSaksnummer());

        ProsessTaskData taskData = new ProsessTaskData(VurderOmVedtakPåvirkerAndreSakerTask.TASKNAME);
        taskData.setPayload(payload);

        taskRepository.lagre(taskData);
    }
}
