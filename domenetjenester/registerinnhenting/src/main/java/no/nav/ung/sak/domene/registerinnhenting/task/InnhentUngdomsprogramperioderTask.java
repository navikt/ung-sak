package no.nav.ung.sak.domene.registerinnhenting.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramTjeneste;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FagsakperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ProsessTask(InnhentUngdomsprogramperioderTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentUngdomsprogramperioderTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.ungdomsprogramperioder";
    private static final Logger LOGGER = LoggerFactory.getLogger(InnhentUngdomsprogramperioderTask.class);
    private UngdomsprogramTjeneste ungdomsprogramTjeneste;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private FagsakRepository fagsakRepository;

    InnhentUngdomsprogramperioderTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentUngdomsprogramperioderTask(BehandlingRepositoryProvider repositoryProvider,
                                             BehandlingLåsRepository behandlingLåsRepository,
                                             UngdomsprogramTjeneste ungdomsprogramTjeneste,
                                             UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, FagsakRepository fagsakRepository) {
        super(repositoryProvider.getBehandlingRepository(), behandlingLåsRepository);
        this.ungdomsprogramTjeneste = ungdomsprogramTjeneste;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        LOGGER.info("Innhenter ungdomsprogramperioder for behandling: {}", behandling.getId());
        ungdomsprogramTjeneste.innhentOpplysninger(behandling);
        final var periodeTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());
        if (!periodeTidslinje.isEmpty()) {
            fagsakRepository.utvidPeriode(behandling.getFagsakId(), periodeTidslinje.getMinLocalDate(), FagsakperiodeUtleder.finnTomDato(periodeTidslinje.getMinLocalDate(), periodeTidslinje));
        }
    }
}
