package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.vedtak.VedtakHendelse;


@ApplicationScoped
@ActivateRequestContext
@Transactional
public class VedtaksHendelseHåndterer {

    private static final Logger log = LoggerFactory.getLogger(VedtaksHendelseHåndterer.class);
    private static final ObjectMapper OM = new ObjectMapper();
    private ProsessTaskRepository taskRepository;
    private BehandlingRepository behandlingRepository;

    public VedtaksHendelseHåndterer() {
    }

    @Inject
    public VedtaksHendelseHåndterer(ProsessTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    void handleMessage(String key, String payload) {
        log.debug("Mottatt ytelse-vedtatt hendelse med key='{}', payload={}", key, payload);
        ProsessTaskData taskData = new ProsessTaskData(VurderRevurderingAndreSøknaderTask.TASKNAME);

        VedtakHendelse vh;
        try {
            vh = OM.readValue(payload, VedtakHendelse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException();  //FIXME: exceptiontype for parsing?
        }

        Behandling behandling = behandlingRepository.hentBehandling(vh.getBehandlingId());
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        taskRepository.lagre(taskData);
    }
}
