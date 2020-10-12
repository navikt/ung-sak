package no.nav.k9.sak.domene.vedtak.midlertidig;

import static no.nav.k9.sak.domene.vedtak.midlertidig.PublisereHistoriskeVedtakHendelserTask.TASKTYPE;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
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
                                                  BehandlingLåsRepository behandlingLåsRepository,
                                                  ProsessTaskRepository prosessTaskRepository) {
        this.vedtakRepository = vedtakRepository;
        this.behandlingLåsRepository = behandlingLåsRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var vedtakSomSkalPubliseres = vedtakRepository.hentBehandlingVedtakSomIkkeErPublisert(1).stream().findFirst();

        vedtakSomSkalPubliseres.ifPresent((vedtak) -> {
            behandlingLåsRepository.taLås(vedtak.getBehandlingId());
            opprettTaskForPubliseringAvVedtak(vedtak.getBehandlingId());

            opprettTaskForNyIterasjonAvHistoriskeVedtakhendelserTask();
        });
    }

    private void opprettTaskForNyIterasjonAvHistoriskeVedtakhendelserTask() {
        final ProsessTaskData taskData = new ProsessTaskData(TASKTYPE);
        taskData.setNesteKjøringEtter(LocalDateTime.now().plus(100, ChronoUnit.MILLIS));
        prosessTaskRepository.lagre(taskData);
    }

    private void opprettTaskForPubliseringAvVedtak(Long behandlingId) {
        final ProsessTaskData taskData = new ProsessTaskData(PubliserVedtakHendelseTask.TASKTYPE);
        taskData.setProperty("behandlingId", behandlingId.toString());
        prosessTaskRepository.lagre(taskData);
    }
}
