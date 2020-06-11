package no.nav.k9.sak.behandling.prosessering;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.k9.sak.behandling.prosessering.task.StartBehandlingTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

@Dependent
public class ProsesseringAsynkTjeneste {

    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    private final Set<ProsessTaskStatus> ferdigStatuser = Set.of(ProsessTaskStatus.FERDIG, ProsessTaskStatus.KJOERT);

    ProsesseringAsynkTjeneste() {
        // For CDI proxy
    }

    @Inject
    public ProsesseringAsynkTjeneste(FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    public Map<String, ProsessTaskData> sjekkProsessTaskPågår(Long fagsakId, String behandlingId, String gruppe) {

        Map<String, List<ProsessTaskData>> statusProsessTasks = sjekkStatusProsessTasksGrouped(fagsakId, behandlingId, gruppe);

        Map<String, ProsessTaskData> nestePerGruppe = nesteProsessTaskPerGruppe(statusProsessTasks);

        if (angittGruppeErFerdig(gruppe, nestePerGruppe)) {
            nestePerGruppe = nesteProsessTaskPerGruppe(sjekkStatusProsessTasksGrouped(fagsakId, behandlingId, null));
        }

        return nestePerGruppe;
    }

    /**
     * Sjekker om prosess tasks pågår nå for angitt behandling. Returnerer neste utestående tasks per gruppe for status (KLAR, FEILET, VENTER),
     * men ikke FERDIG, SUSPENDERT (unntatt der matcher angitt gruppe)
     *
     * Hvis gruppe angis sjekkes kun angitt gruppe. Dersom denne er null returneres status for alle åpne grupper (ikke-ferdig) for angitt
     * behandling. Tasks som er {@link ProsessTaskStatus#FERDIG} ignoreres i resultatet når gruppe ikke er angitt.
     */
    public Map<String, ProsessTaskData> sjekkProsessTaskPågårForBehandling(Behandling behandling, String gruppe) {
        return sjekkProsessTaskPågår(behandling.getFagsakId(), String.valueOf(behandling.getId()), gruppe);
    }

    /**
     * Merge ny gruppe med eksisterende, hvis tidligere gruppe er i gang ignoreres input gruppe her. Hvis tidligere gruppe har feil kastes
     * exception.
     * For å merge sees det på individuelle tasks inne i gruppen (da gruppe id kan være forskjellig uansett).
     */
    public String lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(Long fagsakId, String behandlingId, ProsessTaskGruppe gruppe) {
        return fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, gruppe);
    }

    private Map<String, ProsessTaskData> nesteProsessTaskPerGruppe(Map<String, List<ProsessTaskData>> tasks) {
        // velg top task per gruppe
        Map<String, ProsessTaskData> topTaskPerGruppe = tasks.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .map(e -> e.getValue()
                .stream()
                .sorted(
                    Comparator.comparing(ProsessTaskData::getSekvens)
                        .thenComparing(Comparator.comparing(ProsessTaskData::getStatus).reversed()) /* NB: avhenger av enum ordinal! */)
                .findFirst().get())
            .collect(Collectors.toMap(ProsessTaskData::getGruppe, Function.identity()));

        return topTaskPerGruppe;
    }

    private boolean angittGruppeErFerdig(String gruppe, Map<String, ProsessTaskData> nestePerGruppe) {
        return gruppe != null
            && (nestePerGruppe.isEmpty()
                || (nestePerGruppe.size() == 1
                    && nestePerGruppe.containsKey(gruppe)
                    && ferdigStatuser.contains(nestePerGruppe.get(gruppe).getStatus())));
    }

    private Map<String, List<ProsessTaskData>> sjekkStatusProsessTasksGrouped(Long fagsakId, String behandlingId, String gruppe) {
        List<ProsessTaskData> tasks = fagsakProsessTaskRepository.sjekkStatusProsessTasks(fagsakId, behandlingId, gruppe);
        return tasks.stream().collect(Collectors.groupingBy(ProsessTaskData::getGruppe));
    }

    /**
     * Kjør prosess asynkront (i egen prosess task) videre.
     * 
     * @return gruppe assignet til prosess task
     */
    public String asynkStartBehandlingProsess(Behandling behandling) {
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        ProsessTaskData taskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setCallIdFraEksisterende();
        gruppe.addNesteSekvensiell(taskData);
        return fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), String.valueOf(behandling.getId()), gruppe);
    }

    /**
     * Kjør prosess asynkront (i egen prosess task) videre.
     * 
     * @return gruppe assignet til prosess task
     */
    public String asynkProsesserBehandlingMergeGruppe(Behandling behandling) {
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        ProsessTaskData taskData = new ProsessTaskData(FortsettBehandlingTask.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setCallIdFraEksisterende();
        gruppe.addNesteSekvensiell(taskData);
        return fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), String.valueOf(behandling.getId()), gruppe);
    }

}
