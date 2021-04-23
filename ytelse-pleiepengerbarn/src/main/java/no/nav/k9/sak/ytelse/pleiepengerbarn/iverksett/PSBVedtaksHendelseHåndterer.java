package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.vedtak.VedtakHendelse;


@ApplicationScoped
@ActivateRequestContext
@Transactional
public class PSBVedtaksHendelseHåndterer {

    private static final Logger log = LoggerFactory.getLogger(PSBVedtaksHendelseHåndterer.class);
    private ProsessTaskRepository taskRepository;
    private BehandlingRepository behandlingRepository;

    public PSBVedtaksHendelseHåndterer() {
    }

    @Inject
    public PSBVedtaksHendelseHåndterer(ProsessTaskRepository taskRepository, BehandlingRepository behandlingRepository) {
        this.taskRepository = taskRepository;
        this.behandlingRepository = behandlingRepository;
    }

    void handleMessage(String key, String payload) {
        log.debug("Mottatt ytelse-vedtatt hendelse med key='{}', payload={}", key, payload);
        ProsessTaskData taskData = new ProsessTaskData(VurderRevurderingAndreSøknaderTask.TASKNAME);

        VedtakHendelse vh = JsonObjectMapper.fromJson(payload, VedtakHendelse.class);

        if (vh.getFagsakYtelseType().equals(FagsakYtelseType.PSB)) {
            Behandling behandling = behandlingRepository.hentBehandling(vh.getBehandlingId());
            taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

            taskRepository.lagre(taskData);
        }
    }
}
