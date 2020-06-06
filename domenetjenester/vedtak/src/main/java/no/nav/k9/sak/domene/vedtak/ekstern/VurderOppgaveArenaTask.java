package no.nav.k9.sak.domene.vedtak.ekstern;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(VurderOppgaveArenaTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOppgaveArenaTask extends BehandlingProsessTask {

    private static final Logger log = LoggerFactory.getLogger(VurderOppgaveArenaTask.class);

    public static final String TASKTYPE = "iverksetteVedtak.oppgaveArena";

    private VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphøre;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private BehandlingRepository behandlingRepository;

    VurderOppgaveArenaTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOppgaveArenaTask(BehandlingRepositoryProvider repositoryProvider,
                                  VurderOmArenaYtelseSkalOpphøre vurdereOmArenaYtelseSkalOpphøre,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.vurdereOmArenaYtelseSkalOpphøre = vurdereOmArenaYtelseSkalOpphøre;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling);
        
        AktørId aktørId = new AktørId(prosessTaskData.getAktørId());
        LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
        vurdereOmArenaYtelseSkalOpphøre.opprettOppgaveHvisArenaytelseSkalOpphøre(behandlingId, aktørId, skjæringstidspunkt);
        log.info("VurderOppgaveArenaTask: Vurderer for behandling: {}", behandlingId); //$NON-NLS-1$
    }
}
