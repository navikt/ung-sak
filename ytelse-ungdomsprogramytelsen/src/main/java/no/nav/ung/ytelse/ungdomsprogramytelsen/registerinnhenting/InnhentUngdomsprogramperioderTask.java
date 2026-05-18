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
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.stream.Collectors;

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

    InnhentUngdomsprogramperioderTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentUngdomsprogramperioderTask(BehandlingRepositoryProvider repositoryProvider,
                                             BehandlingLåsRepository behandlingLåsRepository,
                                             UngdomsprogramTjeneste ungdomsprogramTjeneste,
                                             UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                             FagsakRepository fagsakRepository,
                                             ProsessTriggereRepository prosessTriggereRepository) {
        super(repositoryProvider.getBehandlingRepository(), behandlingLåsRepository);
        this.ungdomsprogramTjeneste = ungdomsprogramTjeneste;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    @Override
    public void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        LOGGER.info("Innhenter ungdomsprogramperioder for behandling: {}", behandling.getId());
        ungdomsprogramTjeneste.innhentOpplysninger(behandling);
        final var periodeTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());
        if (!periodeTidslinje.isEmpty()) {
            var fom = periodeTidslinje.getMinLocalDate();
            var harForlengetPeriode = ungdomsprogramPeriodeTjeneste.finnHarForlengetPeriode(behandling.getId());
            var maksDato = ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(behandling.getId()).orElse(null);
            var tom = FagsakperiodeUtleder.finnTomDato(fom, periodeTidslinje, harForlengetPeriode, maksDato);
            fagsakRepository.utvidPeriode(behandling.getFagsakId(), fom, tom);

            // Ved forlenget periode: kapp den initielle trigger-perioden (som ble satt med åpen tom
            // ved opprettelse av revurderingen) til siste maksdato lagret på grunnlaget.
            if (harForlengetPeriode && maksDato != null
                && behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM)) {
                kappForlengetPeriodeTrigger(behandling, maksDato);
            }
        }
    }

    private void kappForlengetPeriodeTrigger(Behandling behandling, LocalDate maksDato) {
        var grunnlag = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        if (grunnlag.isEmpty()) {
            return;
        }
        var nyePerioder = grunnlag.get().getTriggere().stream()
            .filter(t -> BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM.equals(t.getÅrsak()))
            .map(t -> DatoIntervallEntitet.fraOgMedTilOgMed(
                t.getPeriode().getFomDato(),
                t.getPeriode().getTomDato().isAfter(maksDato) ? maksDato : t.getPeriode().getTomDato()))
            .collect(Collectors.toSet());
        if (!nyePerioder.isEmpty()) {
            LOGGER.info("Kapper forlenget-periode-trigger på behandling={} til tom={} basert på maksdato lagret på grunnlaget.", behandling.getId(), maksDato);
            prosessTriggereRepository.erstattTriggereForÅrsak(
                behandling.getId(),
                BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM,
                nyePerioder);
        }
    }
}
