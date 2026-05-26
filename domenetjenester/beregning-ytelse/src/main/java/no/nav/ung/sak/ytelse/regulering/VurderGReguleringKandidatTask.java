package no.nav.ung.sak.ytelse.regulering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

@FagsakProsesstaskRekkefølge(gruppeSekvens = false) // Trenger ikke blokkere andre tasks på saken
@ProsessTask(VurderGReguleringKandidatTask.TASKTYPE)
@ApplicationScoped
public class VurderGReguleringKandidatTask extends FagsakProsessTask {

    public static final String TASKTYPE = "gregulering.kandidatUtprøving";
    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";
    public static final String PERIODER = "perioder";
    public static final String BEHANDLINGSKONTROLL_OPPRETT_REVURDERING_ELLER_DIFF_TASK = "behandlingskontroll.opprettRevurderingEllerDiff";
    public static final String BEHANDLING_ARSAK = "behandlingArsak";
    private static final Logger log = LoggerFactory.getLogger(VurderGReguleringKandidatTask.class);
    private Instance<PerioderForGReguleringUtleder> kandidaterForGReguleringTjeneste;
    private ProsessTaskTjeneste taskRepository;
    private BehandlingRepository behandlingRepository;

    VurderGReguleringKandidatTask() {
        // CDI
    }

    @Inject
    public VurderGReguleringKandidatTask(FagsakLåsRepository fagsakLåsRepository,
                                         @Any Instance<PerioderForGReguleringUtleder> kandidaterForGReguleringTjeneste,
                                         ProsessTaskTjeneste taskRepository, BehandlingRepository behandlingRepository) {
        super(fagsakLåsRepository, null);
        this.kandidaterForGReguleringTjeneste = kandidaterForGReguleringTjeneste;
        this.taskRepository = taskRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var fomProperty = prosessTaskData.getPropertyValue(PERIODE_FOM);
        if (fomProperty.startsWith("\"")) {
            fomProperty = fomProperty.substring(1, fomProperty.length() - 1);
        }
        var fom = LocalDate.parse(fomProperty);
        var tomProperty = prosessTaskData.getPropertyValue(PERIODE_TOM);
        if (tomProperty.startsWith("\"")) {
            tomProperty = tomProperty.substring(1, tomProperty .length() - 1);
        }
        var tom = LocalDate.parse(tomProperty);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);


        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(prosessTaskData.getFagsakId()).orElseThrow(() -> new IllegalStateException("Forventer å finne behandling"));

        var perioderSomSkalGReguleres = PerioderForGReguleringUtleder.finnTjeneste(sisteBehandling.getFagsak().getYtelseType(), kandidaterForGReguleringTjeneste)
            .utledPerioderForGRegulering(sisteBehandling, periode);

        if (!perioderSomSkalGReguleres.isEmpty()) {
            log.info("Fagsaken skal g-reguleres for følgende perioder " + perioderSomSkalGReguleres);
            var data =  ProsessTaskData.forTaskType(new TaskType(BEHANDLINGSKONTROLL_OPPRETT_REVURDERING_ELLER_DIFF_TASK));
            data.setFagsakId(prosessTaskData.getFagsakId());
            data.setProperty(BEHANDLING_ARSAK, BehandlingÅrsakType.RE_SATS_REGULERING.getKode());
            var periodeString = lagPeriodeString(perioderSomSkalGReguleres);
            data.setProperty(PERIODER, periodeString);
            taskRepository.lagre(data);
        } else {
            log.info("Fagsaken trenger IKKE g-reguleres");
        }
    }

    private static String lagPeriodeString(Set<DatoIntervallEntitet> perioderSomSkalGReguleres) {
        return perioderSomSkalGReguleres.stream()
            .map(p -> p.getFomDato().format(DateTimeFormatter.ISO_DATE) + "/" + p.getTomDato().format(DateTimeFormatter.ISO_DATE))
            .reduce((s1, s2) -> s1 + "|" + s2)
            .orElse("");
    }
}
