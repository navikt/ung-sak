package no.nav.k9.sak.domene.vedtak.midlertidig;

import static no.nav.k9.sak.domene.vedtak.midlertidig.PublisereHistoriskeVedtakHendelserTask.TASKTYPE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.domene.vedtak.observer.PubliserVedtakHendelseTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(TASKTYPE)
public class PublisereHistoriskeVedtakHendelserTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "vedtak.etterfyllHistoriske";

    private BehandlingVedtakRepository vedtakRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    @Inject
    public PublisereHistoriskeVedtakHendelserTask(BehandlingVedtakRepository vedtakRepository,
                                                  ProsessTaskRepository prosessTaskRepository,
                                                  BehandlingLåsRepository behandlingLåsRepository) {
        this.vedtakRepository = vedtakRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingLåsRepository = behandlingLåsRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var vedtakSomSkalPubliseres = vedtakRepository.hentBehandlingVedtakSomIkkeErPublisert(100);
        if (vedtakSomSkalPubliseres.isEmpty()) {
            return;
        }

        for (BehandlingVedtak vedtak : vedtakSomSkalPubliseres) {
            var behandlingId = vedtak.getBehandlingId();

            if (behandlingLåsRepository != null) {
                behandlingLåsRepository.taLås(behandlingId);
            }
            opprettTaskForPubliseringAvVedtak(vedtak.getBehandlingId());
        }

        opprettTaskForNyIterasjonAvHistoriskeVedtakhendelserTask();
    }

    private void opprettTaskForNyIterasjonAvHistoriskeVedtakhendelserTask() {
        final ProsessTaskData taskData = new ProsessTaskData(TASKTYPE);
        prosessTaskRepository.lagre(taskData);
    }

    private void opprettTaskForPubliseringAvVedtak(Long behandlingId) {
        final ProsessTaskData taskData = new ProsessTaskData(PubliserVedtakHendelseTask.TASKTYPE);
        taskData.setProperty(PubliserVedtakHendelseTask.KEY, behandlingId.toString());
        prosessTaskRepository.lagre(taskData);
    }
}
