package no.nav.k9.sak.domene.vedtak.midlertidig;

import static no.nav.k9.sak.domene.vedtak.midlertidig.PublisereHistoriskeVedtakHendelserTask.TASKTYPE;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.domene.vedtak.observer.PubliserVedtakHendelseTask;

@ApplicationScoped
@ProsessTask(TASKTYPE)
public class PublisereHistoriskeVedtakHendelserTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "vedtak.etterfyllHistoriske";

    private BehandlingVedtakRepository vedtakRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingLåsRepository behandlingLåsRepository;

    @Inject
    public PublisereHistoriskeVedtakHendelserTask(BehandlingVedtakRepository vedtakRepository,
                                                  BehandlingLåsRepository behandlingLåsRepository,
                                                  ProsessTaskTjeneste taskTjeneste) {
        this.vedtakRepository = vedtakRepository;
        this.behandlingLåsRepository = behandlingLåsRepository;
        this.taskTjeneste = taskTjeneste;
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
        final ProsessTaskData taskData = ProsessTaskData.forProsessTask(PublisereHistoriskeVedtakHendelserTask.class);
        taskData.setNesteKjøringEtter(LocalDateTime.now().plus(100, ChronoUnit.MILLIS));
        taskTjeneste.lagre(taskData);
    }

    private void opprettTaskForPubliseringAvVedtak(Long behandlingId) {
        final ProsessTaskData taskData = ProsessTaskData.forProsessTask(PubliserVedtakHendelseTask.class);
        taskData.setProperty("behandlingId", behandlingId.toString());
        taskTjeneste.lagre(taskData);
    }
}
