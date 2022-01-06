package no.nav.k9.sak.domene.vedtak.observer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;

@ApplicationScoped
public class VedtakFattetEventObserver {

    private ProsessTaskRepository taskRepository;

    public VedtakFattetEventObserver() {
    }

    @Inject
    public VedtakFattetEventObserver(ProsessTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void observerBehandlingVedtak(@Observes BehandlingVedtakEvent event) {
        if (IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())) {
            opprettTaskForPubliseringAvVedtak(event);

            if (erBehandlingAvRettTypeForAbakus(event)) {
                opprettTaskForPubliseringAvVedtakMedYtelse(event);
            }
        }
    }

    private boolean erBehandlingAvRettTypeForAbakus(BehandlingVedtakEvent event) {
        return event.getBehandling().erYtelseBehandling();
    }

    private void opprettTaskForPubliseringAvVedtakMedYtelse(BehandlingVedtakEvent event) {
        final ProsessTaskData taskData = new ProsessTaskData(PubliserVedtattYtelseHendelseTask.TASKTYPE);
        taskData.setBehandling(event.getFagsakId(), event.getBehandlingId(), event.getAktørId().toString());
        taskData.setCallIdFraEksisterende();
        taskRepository.lagre(taskData);
    }

    private void opprettTaskForPubliseringAvVedtak(BehandlingVedtakEvent event) {
        final ProsessTaskData taskData = new ProsessTaskData(PubliserVedtakHendelseTask.TASKTYPE);
        taskData.setBehandling(event.getFagsakId(), event.getBehandlingId(), event.getAktørId().toString());
        taskData.setCallIdFraEksisterende();
        taskRepository.lagre(taskData);
    }
}
