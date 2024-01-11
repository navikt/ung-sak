package no.nav.k9.sak.behandlingslager.fagsak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.jpa.HibernateHints;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskEvent;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe.Entry;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.prosesstask.impl.ProsessTaskEntitet;
import no.nav.k9.prosesstask.impl.TaskManager;

/**
 * Repository for å håndtere kobling mellom Fagsak (og Behandling) mot Prosess Tasks.
 */
@ApplicationScoped
public class FagsakProsessTaskRepository {

    private static final Logger log = LoggerFactory.getLogger(FagsakProsessTaskRepository.class);
    private final Set<ProsessTaskStatus> ferdigStatuser = Set.of(ProsessTaskStatus.FERDIG, ProsessTaskStatus.KJOERT);
    private final Set<String> unikeTaskProperties = Set.of("callId", "parent.");
    private EntityManager em;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private TaskManager taskManager;

    FagsakProsessTaskRepository() {
        // for proxy
    }

    @Inject
    public FagsakProsessTaskRepository(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste, TaskManager taskManager) {
        this.em = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.taskManager = taskManager;
    }

    public void lagre(FagsakProsessTask fagsakProsessTask) {
        ProsessTaskData ptData = prosessTaskTjeneste.finn(fagsakProsessTask.getProsessTaskId());
        log.debug("Linker fagsak[{}] -> prosesstask[{}], tasktype=[{}] gruppeSekvensNr=[{}]", fagsakProsessTask.getFagsakId(), fagsakProsessTask.getProsessTaskId(), ptData.getTaskType(),
            fagsakProsessTask.getGruppeSekvensNr());
        EntityManager em = getEntityManager();
        em.persist(fagsakProsessTask);
        em.flush();
    }

    public Optional<FagsakProsessTask> hent(Long prosessTaskId, boolean lås) {
        TypedQuery<FagsakProsessTask> query = getEntityManager().createQuery("select fpt from FagsakProsessTask fpt where fpt.prosessTaskId=:prosessTaskId",
            FagsakProsessTask.class);
        query.setParameter("prosessTaskId", prosessTaskId);
        if (lås) {
            query.setLockMode(LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void fjern(Long fagsakId, Long prosessTaskId, Long gruppeSekvensNr) {
        ProsessTaskData ptData = prosessTaskTjeneste.finn(prosessTaskId);
        log.debug("Fjerner link fagsak[{}] -> prosesstask[{}], tasktype=[{}] gruppeSekvensNr=[{}]", fagsakId, prosessTaskId, ptData.getTaskType(), gruppeSekvensNr);
        EntityManager em = getEntityManager();
        Query query = em.createNativeQuery("delete from FAGSAK_PROSESS_TASK where prosess_task_id = :prosessTaskId and fagsak_id=:fagsakId");
        query.setParameter("prosessTaskId", prosessTaskId); // NOSONAR
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        query.executeUpdate();
        frigiVeto(ptData);
        em.flush();
    }

    boolean frigiVeto(ProsessTaskData blokkerendeTask) {
        String updateSql = "update PROSESS_TASK SET "
            + " status='KLAR'"
            + ", blokkert_av=NULL"
            + ", siste_kjoering_feil_kode=NULL"
            + ", siste_kjoering_feil_tekst=NULL"
            + ", neste_kjoering_etter=NULL"
            + ", versjon = versjon +1"
            + " WHERE blokkert_av=:id";

        int frigitt = em.createNativeQuery(updateSql)
            .setParameter("id", blokkerendeTask.getId())
            .executeUpdate();

        if (frigitt > 0) {
            log.info("ProsessTask [id={}, taskType={}] SUSPENDERT. Frigitt {} tidligere blokkerte tasks", blokkerendeTask.getId(), blokkerendeTask.taskType(),
                frigitt);
            return true;
        }
        return false; // Har ikke hatt noe veto å frigi
    }

    public List<ProsessTaskData> finnAlleForAngittSøk(Long fagsakId, String behandlingId, String gruppeId, Collection<ProsessTaskStatus> statuser,
                                                      boolean kunGruppeSekvens,
                                                      LocalDateTime nesteKjoeringFraOgMed,
                                                      LocalDateTime nesteKjoeringTilOgMed) {

        List<String> statusNames = statuser.stream().map(ProsessTaskStatus::getDbKode).collect(Collectors.toList());

        // native sql for å håndtere join og subselect,
        // samt cast til hibernate spesifikk håndtering av parametere som kan være NULL
        String sql = "SELECT pt.* FROM PROSESS_TASK pt"
            + " INNER JOIN FAGSAK_PROSESS_TASK fpt ON fpt.prosess_task_id = pt.id"
            + " WHERE pt.status IN (:statuses)"
            + " AND pt.task_gruppe = coalesce(:gruppe, pt.task_gruppe)"
            + (kunGruppeSekvens ? " AND fpt.gruppe_sekvensnr IS NOT NULL" : "") // tar kun hensyn til de som følger rekkefølge av tasks
            + " AND (pt.neste_kjoering_etter IS NULL"
            + "      OR ("
            + "           pt.neste_kjoering_etter >= cast(:nesteKjoeringFraOgMed as timestamp(0)) AND pt.neste_kjoering_etter <= cast(:nesteKjoeringTilOgMed as timestamp(0))"
            + "      ))"
            + " AND fpt.fagsak_id = :fagsakId AND fpt.behandling_id = coalesce(:behandlingId, fpt.behandling_id)";

        @SuppressWarnings("unchecked")
        NativeQuery<ProsessTaskEntitet> query = (NativeQuery<ProsessTaskEntitet>) em
            .createNativeQuery(
                sql,
                ProsessTaskEntitet.class);

        query.setParameter("statuses", statusNames)
            .setParameter("gruppe", gruppeId, StandardBasicTypes.STRING)
            .setParameter("nesteKjoeringFraOgMed", nesteKjoeringFraOgMed) // max oppløsning på neste_kjoering_etter er sekunder
            .setParameter("nesteKjoeringTilOgMed", nesteKjoeringTilOgMed)
            .setParameter("fagsakId", fagsakId) // NOSONAR
            .setParameter("behandlingId", behandlingId, StandardBasicTypes.STRING) // NOSONAR
            .setHint(HibernateHints.HINT_READ_ONLY, "true");

        List<ProsessTaskEntitet> resultList = query.getResultList();
        return tilProsessTask(resultList);
    }

    public String lagreNyGruppe(ProsessTaskData taskData) {
        Optional.ofNullable(MDC.get("prosess_steg")).ifPresent(v -> taskData.setProperty("parent.steg", v));
        return prosessTaskTjeneste.lagre(taskData);
    }

    public String lagreNyGruppe(ProsessTaskGruppe gruppe) {
        Optional.ofNullable(MDC.get("prosess_steg")).ifPresent(v -> gruppe.setProperty("parent.steg", v));
        String nyGruppeId = prosessTaskTjeneste.lagre(gruppe);
        log.info("Lagret gruppe {} med tasker: [{}]. Kjørende task er {}", nyGruppeId, toStringEntry(gruppe.getTasks()), taskManager.getCurrentTask());
        return nyGruppeId;
    }

    public String lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(Long fagsakId, Long behandlingId, ProsessTaskGruppe gruppe) {
        return lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId.toString(), gruppe);
    }

    public String lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(Long fagsakId, String behandlingId, ProsessTaskGruppe gruppe) {
        // Oppretter nye tasks hvis ingen eksisterende har feilet eller er av samme type som nye.
        // Ignorerer hvis eksisterende gruppe er vetoet og matcher tasktyper i ny gruppe
        List<ProsessTaskData> eksisterendeTasks = sjekkStatusProsessTasks(fagsakId, behandlingId, null);

        gruppe.setCallIdFraEksisterende();

        if (eksisterendeTasks.isEmpty()) {
            return lagreNyGruppe(gruppe);
        }

        // hvis noen er FEILET så oppretter vi ikke nye
        Optional<ProsessTaskData> feilet = eksisterendeTasks.stream().filter(t -> t.getStatus().equals(ProsessTaskStatus.FEILET)).findFirst();
        if (feilet.isPresent()) {
            throw new IllegalStateException("Kan ikke opprette gruppe med tasks: [" + toStringEntry(gruppe.getTasks()) + "].  Har allerede feilende task [" + feilet.get() + "]");
        }

        ProsessTaskData currentTaskData = taskManager.getCurrentTask();
        List<ProsessTaskData> nyeTasks = gruppe.getTasks().stream().map(Entry::getTask).toList();

        Set<String> nyeTaskTyper = nyeTasks.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toSet());
        Set<String> eksisterendeTaskTyper = eksisterendeTasks.stream()
            .filter(t -> currentTaskData == null || !Objects.equals(t.getId(), currentTaskData.getId())) // se bort fra oss selv (hvis vi kjører i en task)
            .map(ProsessTaskData::getTaskType).collect(Collectors.toSet());

        var overlappNyeOgEksisterendeTaskTyper = new HashSet<>(eksisterendeTaskTyper);
        overlappNyeOgEksisterendeTaskTyper.retainAll(nyeTaskTyper);

        if (overlappNyeOgEksisterendeTaskTyper.isEmpty()) {
            return lagreNyGruppe(gruppe);
        }

        Set<ProsessTaskData> planlagteTasksBlokkertAvKjørende = eksisterendeTasks.stream()
            .filter(t -> Objects.equals(t.getStatus(), ProsessTaskStatus.VETO) && currentTaskData != null && Objects.equals(currentTaskData.getId(), t.getBlokkertAvProsessTaskId()))
            .collect(Collectors.toSet());
        Set<String> planlagteTaskTyperBlokkertAvKjørende = planlagteTasksBlokkertAvKjørende.stream()
            .map(ProsessTaskData::getTaskType)
            .collect(Collectors.toSet());
        Set<String> blokkerteGrupper = planlagteTasksBlokkertAvKjørende.stream().map(ProsessTaskData::getGruppe).collect(Collectors.toSet());
        Set<ProsessTaskData> ventendeTasksIGruppeMedBlokkert = eksisterendeTasks.stream()
            .filter(t -> currentTaskData == null || !Objects.equals(t.getId(), currentTaskData.getId())) // se bort fra oss selv (hvis vi kjører i en task)
            .filter(t -> blokkerteGrupper.contains(t.getGruppe()))
            .filter(t -> Objects.equals(t.getStatus(), ProsessTaskStatus.KLAR))
            .collect(Collectors.toSet());
        Set<String> ventendeTaskTyperIGruppeMedBlokkert = ventendeTasksIGruppeMedBlokkert.stream()
            .map(ProsessTaskData::getTaskType)
            .collect(Collectors.toSet());

        Set<ProsessTaskData> vetoetEllerVentendeTasks = new HashSet<>(planlagteTasksBlokkertAvKjørende);
        vetoetEllerVentendeTasks.addAll(ventendeTasksIGruppeMedBlokkert);

        Set<String> vetoetEllerVentendeTasksAvSammeTypeSomNy = new HashSet<>(planlagteTaskTyperBlokkertAvKjørende);
        vetoetEllerVentendeTasksAvSammeTypeSomNy.addAll(ventendeTaskTyperIGruppeMedBlokkert);
        vetoetEllerVentendeTasksAvSammeTypeSomNy.retainAll(nyeTaskTyper);
        if (!vetoetEllerVentendeTasksAvSammeTypeSomNy.isEmpty()) {
            log.info("Vetoet eller ventende tasks av samme type som nye: {}", vetoetEllerVentendeTasksAvSammeTypeSomNy);
        }

        if (vetoetEllerVentendeTasksAvSammeTypeSomNy.containsAll(nyeTaskTyper) && taskPropertiesMatcher(vetoetEllerVentendeTasks, nyeTasks)) {
            log.info("Skipper opprettelse av gruppe med tasks: [{}], Har allerede vetoet tasks av samme type [{}], Og ventende tasks i gruppe med vetoet [{}]",
                toStringEntry(gruppe.getTasks()), planlagteTaskTyperBlokkertAvKjørende, ventendeTaskTyperIGruppeMedBlokkert);
            return blokkerteGrupper.stream().findFirst().orElse(null);
        }

        throw new IllegalStateException("Kan ikke opprette gruppe med tasks: [" + toStringEntry(gruppe.getTasks()) + "]"
            + " Har allerede [" + toStringTask(eksisterendeTasks) + "]"
            + " Eksisterende tasktyper hensyntatt [" + eksisterendeTaskTyper + "]");
    }

    private boolean taskPropertiesMatcher(Set<ProsessTaskData> eksisterendeTasks, List<ProsessTaskData> nyeTasks) {
        for (ProsessTaskData ny : nyeTasks) {
            boolean taskMatch = false;
            var propertiesNy = hentRelevanteProperties(ny.getProperties());
            var propNamesNy = propertiesNy.stringPropertyNames();
            for (ProsessTaskData eksisterende : eksisterendeTasks) {
                if (eksisterende.getTaskType().equals(ny.getTaskType())) {
                    var propertiesEksisterende = hentRelevanteProperties(eksisterende.getProperties());
                    if (propertiesNy.size() != propertiesEksisterende.size()) {
                        log.info("Task properties matchet ikke, ny task: {} med properties: {}, eksisterende task: {} med properties: {}", ny.getId(), propNamesNy, eksisterende.getId(), propertiesEksisterende.stringPropertyNames());
                        continue;
                    }

                    boolean propsMatch = true;
                    for (String propName : propNamesNy) {
                        if (!Objects.equals(propertiesNy.getProperty(propName), propertiesEksisterende.getProperty(propName))) {
                            log.info("Task property '{}' matchet ikke, ny task: {}, eksisterende task: {}", propName, ny.getId(), eksisterende.getId());
                            propsMatch = false;
                        }
                    }
                    if (propsMatch) {
                        taskMatch = true;
                        break;
                    }
                }

            }
            if (!taskMatch) {
                return false;
            }
        }
        return true;
    }

    private Properties hentRelevanteProperties(Properties props) {
        Properties relProps = new Properties();
        props.forEach((key, value) -> {
            if (!erUnikProperty((String) key)) {
                relProps.setProperty((String) key, (String) value);
            }
        });
        return relProps;
    }

    private boolean erUnikProperty(String propName) {
        for (String unikPropName : unikeTaskProperties) {
            if (propName.contains(unikPropName)) {
                return true;
            }
        }
        return false;
    }

    private String toStringTask(Collection<ProsessTaskData> tasks) {
        return tasks.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    private String toStringEntry(Collection<Entry> tasks) {
        return tasks.stream().map(t -> t.getTask()).map(Object::toString).collect(Collectors.joining(", "));
    }

    public List<ProsessTaskData> sjekkStatusProsessTasks(Long fagsakId, Long behandlingId, String gruppe) {
        return sjekkStatusProsessTasks(fagsakId, behandlingId.toString(), gruppe);
    }

    public List<ProsessTaskData> sjekkStatusProsessTasks(Long fagsakId, String behandlingId, String gruppe) {
        Objects.requireNonNull(fagsakId, "fagsakId"); // NOSONAR

        // et tidsrom for neste kjøring vi kan ta hensyn til. Det som er lenger ut i fremtiden er ikke relevant her, kun det vi kan forvente
        // kjøres i umiddelbar fremtid. tar i tillegg hensyn til alt som skulle ha vært kjørt tilbake i tid (som har stoppet av en eller annen
        // grunn).
        LocalDateTime fom = Tid.TIDENES_BEGYNNELSE.atStartOfDay();
        LocalDateTime tom = Tid.TIDENES_ENDE.atStartOfDay(); // kun det som forventes kjørt om kort tid.
        boolean kunGruppeSekvens = true;

        EnumSet<ProsessTaskStatus> statuser = EnumSet.allOf(ProsessTaskStatus.class);
        List<ProsessTaskData> tasks = Collections.emptyList();
        if (gruppe != null) {
            tasks = finnAlleForAngittSøk(fagsakId, behandlingId, gruppe, new ArrayList<>(statuser), kunGruppeSekvens, fom, tom);
        }

        if (tasks.isEmpty()) {
            // ignorerer alle ferdig, suspendert, når vi søker blant alle grupper
            statuser.remove(ProsessTaskStatus.FERDIG);
            statuser.remove(ProsessTaskStatus.KJOERT);
            statuser.remove(ProsessTaskStatus.SUSPENDERT);
            tasks = finnAlleForAngittSøk(fagsakId, behandlingId, null, new ArrayList<>(statuser), kunGruppeSekvens, fom, tom);
        }

        return tasks;
    }

    public Optional<FagsakProsessTask> sjekkTillattKjøreFagsakProsessTask(ProsessTaskData ptData) {
        Long fagsakId = ptData.getFagsakId();
        Long prosessTaskId = ptData.getId();
        Optional<FagsakProsessTask> fagsakProsessTaskOpt = hent(prosessTaskId, true);

        if (fagsakProsessTaskOpt.isPresent()) {
            FagsakProsessTask fagsakProsessTask = fagsakProsessTaskOpt.get();
            if (fagsakProsessTask.getGruppeSekvensNr() == null) {
                // Dersom gruppe_sekvensnr er NULL er task alltid tillatt.
                return Optional.empty();
            }
            // Dersom den ikke er NULL er den kun tillatt dersom den matcher laveste gruppe_sekvensnr for Fagsak i
            // FAGSAK_PROSESS_TASK tabell.
            TypedQuery<FagsakProsessTask> query = getEntityManager().createQuery("select fpt from FagsakProsessTask fpt " +
                    "where fpt.fagsakId=:fagsakId and fpt.gruppeSekvensNr is not null " +
                    "order by fpt.gruppeSekvensNr ",
                FagsakProsessTask.class);
            query.setParameter("fagsakId", fagsakId); // NOSONAR
            query.setMaxResults(1);

            FagsakProsessTask førsteFagsakProsessTask = query.getResultList().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Skal ikke være mulig å havne her, da en må i minste finne input task her."));
            if (!førsteFagsakProsessTask.getGruppeSekvensNr().equals(fagsakProsessTask.getGruppeSekvensNr())) {
                // Den første tasken (sortert på gruppe_sekvensnr) er ikke samme som gjeldende => en blokkerende task
                return Optional.of(førsteFagsakProsessTask);
            }
            return Optional.empty();
        } else {
            // fallback, er ikke knyttet til Fagsak - så har ingen formening om hvilken rekkefølge det skal kjøres
            return Optional.empty();
        }

    }

    /**
     * Observerer og vedlikeholder relasjon mellom fagsak og prosess task for enklere søk (dvs. fjerner relasjon når FERDIG).
     */
    public void observeProsessTask(@Observes ProsessTaskEvent ptEvent) {

        Long fagsakId = ptEvent.getFagsakId();
        Long prosessTaskId = ptEvent.getId();
        if (fagsakId == null) {
            return; // do nothing, er ikke relatert til fagsak/behandling
        }

        ProsessTaskStatus status = ptEvent.getNyStatus();

        Optional<FagsakProsessTask> fagsakProsessTaskOpt = hent(prosessTaskId, true);

        if (fagsakProsessTaskOpt.isPresent()) {
            if (ferdigStatuser.contains(status)) {
                // fjern link
                fjern(fagsakId, ptEvent.getId(), fagsakProsessTaskOpt.get().getGruppeSekvensNr());
            }
        }

    }

    protected EntityManager getEntityManager() {
        return em;
    }

    private List<ProsessTaskData> tilProsessTask(List<ProsessTaskEntitet> resultList) {
        return resultList.stream().map(ProsessTaskEntitet::tilProsessTask).collect(Collectors.toList());
    }

    /**
     * Sett feilet prosesstasks som er koblet til fagsak+behandling til suspendert.
     */
    public void settFeiletTilSuspendert(Long fagsakId, Long behandlingId) {

        Set<ProsessTaskStatus> feiletStatus = EnumSet.of(ProsessTaskStatus.FEILET);

        var skalSuspenderes = finnAlleForAngittSøk(fagsakId, String.valueOf(behandlingId), null, feiletStatus, false, Tid.TIDENES_BEGYNNELSE.atStartOfDay(),
            Tid.TIDENES_ENDE.plusDays(1).atStartOfDay());
        if (!skalSuspenderes.isEmpty()) {
            em.flush(); // flush alt annet
            for (var s : skalSuspenderes) {
                em.createNativeQuery("update prosess_task p set status = :nyStatus where p.id = :pid")
                    .setParameter("nyStatus", ProsessTaskStatus.SUSPENDERT.getDbKode())
                    .setParameter("pid", s.getId())
                    .executeUpdate();
                em.flush();
                Optional<FagsakProsessTask> fagsakProsessTaskOpt = hent(s.getId(), true);
                fagsakProsessTaskOpt.ifPresent(task -> fjern(fagsakId, s.getId(), task.getGruppeSekvensNr()));
            }

        }

    }

    /**
     * Sletter prosesstasks som er koblet til fagsak+behandling og er ikke kjørt.
     */
    public void ryddProsessTasks(Long fagsakId, Long behandlingId) {

        Set<ProsessTaskStatus> uferdigStatuser = EnumSet.complementOf(EnumSet.of(ProsessTaskStatus.FERDIG, ProsessTaskStatus.KJOERT));

        var skalSlettes = finnAlleForAngittSøk(fagsakId, String.valueOf(behandlingId), null, uferdigStatuser, false, Tid.TIDENES_BEGYNNELSE.atStartOfDay(),
            Tid.TIDENES_ENDE.plusDays(1).atStartOfDay());
        if (!skalSlettes.isEmpty()) {
            em.flush(); // flush alt annet
            em.createNativeQuery("delete from fagsak_prosess_task fpt where fpt.fagsak_id=:fagsakId and fpt.behandling_id=:behandlingId")
                .setParameter("fagsakId", fagsakId)
                .setParameter("behandlingId", "'" + behandlingId + "'")
                .executeUpdate();
            em.flush();
            for (var s : skalSlettes) {
                em.createNativeQuery("delete from prosess_task p where p.id = :pid")
                    .setParameter("pid", s.getId())
                    .executeUpdate();
                em.flush();
            }
        }

    }

    public List<ProsessTaskData> finnAlleÅpneTasksForAngittSøk(Long fagsakId, Long behandlingId, String gruppe) {
        Set<ProsessTaskStatus> uferdigStatuser = EnumSet.complementOf(EnumSet.of(ProsessTaskStatus.FERDIG, ProsessTaskStatus.KJOERT));
        var fom = Tid.TIDENES_BEGYNNELSE.atStartOfDay();
        var tom = Tid.TIDENES_ENDE.plusDays(1).atStartOfDay();
        return finnAlleForAngittSøk(fagsakId, behandlingId.toString(), gruppe, uferdigStatuser, true, fom, tom);
    }

}
