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

    @Inject
    public PublisereHistoriskeVedtakHendelserTask(BehandlingVedtakRepository vedtakRepository,
                                                  ProsessTaskRepository prosessTaskRepository) {
        this.vedtakRepository = vedtakRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var vedtakSomSkalPubliseres = vedtakRepository.hentBehandlingVedtakSomIkkeErPublisert(100);
        if (vedtakSomSkalPubliseres.isEmpty()) {
            return;
        }

        for (BehandlingVedtak vedtak : vedtakSomSkalPubliseres) {
            var behandlingId = vedtak.getBehandlingId();

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
