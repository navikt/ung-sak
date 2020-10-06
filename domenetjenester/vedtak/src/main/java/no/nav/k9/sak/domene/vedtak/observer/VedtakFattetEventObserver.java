package no.nav.k9.sak.domene.vedtak.observer;

import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vedtak.IverksettingStatus;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class VedtakFattetEventObserver {

    private ProsessTaskRepository taskRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository vedtakRepository;

    public VedtakFattetEventObserver() {
    }

    @Inject
    public VedtakFattetEventObserver(ProsessTaskRepository taskRepository, BehandlingRepository behandlingRepository, BehandlingVedtakRepository vedtakRepository) {
        this.taskRepository = taskRepository;
        this.behandlingRepository = behandlingRepository;
        this.vedtakRepository = vedtakRepository;
    }

    public void observerBehandlingVedtak(@Observes BehandlingVedtakEvent event) {
        if (IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())) {
            opprettTaskForPubliseringAvVedtak(event.getBehandlingId());

            if (erBehandlingAvRettTypeForAbakus(event.getBehandlingId())) {
                opprettTaskForPubliseringAvVedtakMedYtelse(event.getBehandlingId());
            }
        }
    }

    private boolean erBehandlingAvRettTypeForAbakus(Long behandlingId) {
        final Optional<Behandling> optBehandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingId);
        if (optBehandling.isPresent()) {
            final Behandling behandling = optBehandling.get();
            if (behandling.erYtelseBehandling()) {
                final VedtakResultatType resultatType = vedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingId)
                    .map(BehandlingVedtak::getVedtakResultatType)
                    .orElse(VedtakResultatType.AVSLAG);

                return Set.of(VedtakResultatType.INNVILGET, VedtakResultatType.DELVIS_INNVILGET, VedtakResultatType.OPPHØR).contains(resultatType);
            }
        }
        return false;
    }

    private void opprettTaskForPubliseringAvVedtakMedYtelse(Long behandlingId) {
        final ProsessTaskData taskData = new ProsessTaskData(PubliserVedtattYtelseHendelseTask.TASKTYPE);
        taskData.setProperty(PubliserVedtattYtelseHendelseTask.KEY, behandlingId.toString());
        taskData.setCallIdFraEksisterende();
        taskRepository.lagre(taskData);
    }

    private void opprettTaskForPubliseringAvVedtak(Long behandlingId) {
        final ProsessTaskData taskData = new ProsessTaskData(PubliserVedtakHendelseTask.TASKTYPE);
        taskData.setProperty(PubliserVedtakHendelseTask.KEY, behandlingId.toString());
        taskData.setCallIdFraEksisterende();
        taskRepository.lagre(taskData);
    }
}
