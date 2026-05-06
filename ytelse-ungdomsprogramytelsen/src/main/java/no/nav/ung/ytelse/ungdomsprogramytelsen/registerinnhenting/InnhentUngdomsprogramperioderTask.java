package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinnhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
@ProsessTask(InnhentUngdomsprogramperioderTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentUngdomsprogramperioderTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.ungdomsprogramperioder";
    private static final Logger LOGGER = LoggerFactory.getLogger(InnhentUngdomsprogramperioderTask.class);
    private UngdomsprogramTjeneste ungdomsprogramTjeneste;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private FagsakRepository fagsakRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    InnhentUngdomsprogramperioderTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentUngdomsprogramperioderTask(BehandlingRepositoryProvider repositoryProvider,
                                             BehandlingLåsRepository behandlingLåsRepository,
                                             UngdomsprogramTjeneste ungdomsprogramTjeneste,
                                             UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                             FagsakRepository fagsakRepository,
                                             ProsessTriggereRepository prosessTriggereRepository,
                                             UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        super(repositoryProvider.getBehandlingRepository(), behandlingLåsRepository);
        this.ungdomsprogramTjeneste = ungdomsprogramTjeneste;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        LOGGER.info("Innhenter ungdomsprogramperioder for behandling: {}", behandling.getId());
        ungdomsprogramTjeneste.innhentOpplysninger(behandling);

        // Ved automatisk opphør: klipp perioden ved maksdato fra prosess-triggere
        if (behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_VARSEL_AUTOMATISK_OPPHOR)) {
            klippPeriodeVedMaksdato(behandling);
        }

        final var periodeTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());
        if (!periodeTidslinje.isEmpty()) {
            var fom = periodeTidslinje.getMinLocalDate();
            var harUtvidetKvote = ungdomsprogramPeriodeTjeneste.finnHarUtvidetKvote(behandling.getId());
            var tom = FagsakperiodeUtleder.finnTomDato(fom, periodeTidslinje, harUtvidetKvote);
            fagsakRepository.utvidPeriode(behandling.getFagsakId(), fom, tom);
        }
    }

    /**
     * Klipper programperioden ved maksdato hentet fra prosess-triggere.
     * Registeret har kanskje ikke oppdatert perioden ennå (varselet sendes før maksdato),
     * så vi setter tom = maksdato eksplisitt.
     */
    private void klippPeriodeVedMaksdato(Behandling behandling) {
        var maksdato = prosessTriggereRepository.hentGrunnlag(behandling.getId())
            .stream()
            .flatMap(pt -> pt.getTriggere().stream())
            .filter(t -> t.getÅrsak() == BehandlingÅrsakType.RE_VARSEL_AUTOMATISK_OPPHOR)
            .map(Trigger::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .findFirst()
            .orElse(null);

        if (maksdato == null) {
            LOGGER.warn("Fant ingen maksdato i prosess-triggere for behandling {}", behandling.getId());
            return;
        }

        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
        if (grunnlag.isEmpty()) {
            return;
        }

        var perioder = grunnlag.get().getUngdomsprogramPerioder().getPerioder();
        var oppdatertePerioder = perioder.stream()
            .map(p -> {
                if (p.getPeriode().getTomDato().isAfter(maksdato)) {
                    LOGGER.info("Klipper programperiode for behandling {} fra tom={} til maksdato={}", behandling.getId(), p.getPeriode().getTomDato(), maksdato);
                    return new UngdomsprogramPeriode(p.getPeriode().getFomDato(), maksdato);
                }
                return p;
            })
            .toList();

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), oppdatertePerioder);
    }
}
