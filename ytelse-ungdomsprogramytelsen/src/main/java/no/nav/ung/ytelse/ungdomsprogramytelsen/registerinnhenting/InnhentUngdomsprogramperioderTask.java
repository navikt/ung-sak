package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinnhenting;

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
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
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
                                             UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                             FagsakRepository fagsakRepository) {
        super(repositoryProvider.getBehandlingRepository(), behandlingLåsRepository);
        this.ungdomsprogramTjeneste = ungdomsprogramTjeneste;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        LOGGER.info("Innhenter ungdomsprogramperioder for behandling={}, fagsakId={}, behandlingsårsaker={}",
            behandling.getId(), behandling.getFagsakId(), behandling.getBehandlingÅrsakerTyper());
        ungdomsprogramTjeneste.innhentOpplysninger(behandling);

        // Perioder fra register lagres uendret (kan ha tom=TIDENES_ENDE for åpne perioder).
        // Konsumenter som trenger perioder begrenset til periodeMaksDato bruker
        // UngdomsprogramPeriodeTjeneste#finnPerioderKappetMotMaksdato.
        // Fagsakperioden beregnes korrekt via FagsakperiodeUtleder som bruker periodeMaksDato.

        final var periodeTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());
        if (!periodeTidslinje.isEmpty()) {
            var fom = periodeTidslinje.getMinLocalDate();
            var harForlengetPeriode = ungdomsprogramPeriodeTjeneste.finnHarForlengetPeriode(behandling.getId());
            var maksDato = ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(behandling.getId()).orElse(null);
            var tom = FagsakperiodeUtleder.finnTomDato(fom, periodeTidslinje, harForlengetPeriode, maksDato);
            LOGGER.info("Utvider fagsakperiode for behandling={}: fom={}, tom={}, harForlengetPeriode={}, periodeMaksDato={}",
                behandling.getId(), fom, tom, harForlengetPeriode, maksDato);
            fagsakRepository.utvidPeriode(behandling.getFagsakId(), fom, tom);
        } else {
            LOGGER.info("Ingen ungdomsprogramperioder funnet for behandling={}", behandling.getId());
        }
    }

}
