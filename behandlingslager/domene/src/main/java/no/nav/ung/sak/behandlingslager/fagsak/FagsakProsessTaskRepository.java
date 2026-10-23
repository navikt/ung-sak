package no.nav.ung.sak.behandlingslager.fagsak;

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
                                                      boolean kunGruppeSekvens) {

        List<String> statusNames = statuser.stream().map(ProsessTaskStatus::getDbKode).collect(Collectors.toList());

        // native sql for å håndtere join og subselect,
        // samt cast til hibernate spesifikk håndtering av parametere som kan være NULL
        String sql = """
            SELECT pt.* FROM PROSESS_TASK pt
             INNER JOIN FAGSAK_PROSESS_TASK fpt ON fpt.prosess_task_id = pt.id
             WHERE pt.status IN (:statuses)
             AND pt.task_gruppe = coalesce(:gruppe, pt.task_gruppe)
             AND fpt.fagsak_id = :fagsakId AND fpt.behandling_id = coalesce(:behandlingId, fpt.behandling_id)
            """
            + (kunGruppeSekvens ? " AND fpt.gruppe_sekvensnr IS NOT NULL" : ""); // tar kun hensyn til de som følger rekkefølge av tasks

        @SuppressWarnings("unchecked")
        NativeQuery<ProsessTaskEntitet> query = (NativeQuery<ProsessTaskEntitet>) em
            .createNativeQuery(
                sql,
                ProsessTaskEntitet.class);

        query.setParameter("statuses", statusNames)
            .setParameter("gruppe", gruppeId, StandardBasicTypes.STRING)
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

        Set<String> overlappNyeOgEksisterendeTaskTyper = new HashSet<>(eksisterendeTaskTyper);
        overlappNyeOgEksisterendeTaskTyper.retainAll(nyeTaskTyper);

        if (overlappNyeOgEksisterendeTaskTyper.isEmpty()) {
            return lagreNyGruppe(gruppe);
        }

        Set<ProsessTaskData> planlagteTasksBlokkertAvKjørende = eksisterendeTasks.stream()
            .filter(t -> Objects.equals(t.getStatus(), ProsessTaskStatus.VETO) && currentTaskData != null && Objects.equals(currentTaskData.getId(), t.getBlokkertAvProsessTaskId()))
            .collect(Collectors.toSet());
        Set<String> blokkerteGrupper = planlagteTasksBlokkertAvKjørende.stream().map(ProsessTaskData::getGruppe).collect(Collectors.toSet());
        Set<ProsessTaskData> ventendeTasksIGruppeMedBlokkert = eksisterendeTasks.stream()
            .filter(t -> currentTaskData == null || !Objects.equals(t.getId(), currentTaskData.getId())) // se bort fra oss selv (hvis vi kjører i en task)
            .filter(t -> blokkerteGrupper.contains(t.getGruppe()))
            .filter(t -> Objects.equals(t.getStatus(), ProsessTaskStatus.KLAR))
            .collect(Collectors.toSet());

        Set<ProsessTaskData> vetoetEllerVentendeTasks = new HashSet<>(planlagteTasksBlokkertAvKjørende);
        vetoetEllerVentendeTasks.addAll(ventendeTasksIGruppeMedBlokkert);

        if (!vetoetEllerVentendeTasks.isEmpty() && nyeTaskerMatcherEksisterende(vetoetEllerVentendeTasks, nyeTasks)) {
            log.info("Skipper opprettelse av gruppe med tasks: [{}], Har allerede vetoet tasks av samme type [{}], Og ventende tasks i gruppe med vetoet [{}]",
                toStringEntry(gruppe.getTasks()),
                planlagteTasksBlokkertAvKjørende.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toSet()),
                ventendeTasksIGruppeMedBlokkert.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toSet()));
            return blokkerteGrupper.stream().findFirst().orElse(null);
        }

        throw new IllegalStateException("Kan ikke opprette gruppe med tasks: [" + toStringEntry(gruppe.getTasks()) + "]"
            + " Har allerede [" + toStringTask(eksisterendeTasks) + "]"
            + " Eksisterende tasktyper hensyntatt [" + eksisterendeTaskTyper + "]");
    }

    /**
     * Her sjekker vi om taskene vi prøver å opprette matcher tasker som er vetoet av den kjørende tasken.
     * Dette gjør vi for å unngå "deadlock" der kjørende task ikke får lov til opprette nye tasker, fordi det finnes tasker som den selv har vetoet.
     * Dersom alle taskene vi prøver å opprette finnes blant de som er vetoet, så trenger vi ikke opprette de nye.
     * Det vi vil gjøre da er å la kjørende task kjøre ferdig uten å opprette nye, slik at de som var vetoet kan kjøre.
    */
    private boolean nyeTaskerMatcherEksisterende(Set<ProsessTaskData> eksisterendeTasks, List<ProsessTaskData> nyeTasks) {
        for (ProsessTaskData ny : nyeTasks) {
            boolean taskMatch = false;
            final var propertiesNy = hentRelevanteProperties(ny.getProperties());
            final String taskType = ny.getTaskType();

            for (ProsessTaskData eksisterende : eksisterendeTasks) {
                if (eksisterende.getTaskType().equals(taskType)) {
                    final var propertiesEksisterende = hentRelevanteProperties(eksisterende.getProperties());
                    håndterSpesialtilfelleFortsettBehandling(eksisterende, eksisterendeTasks, propertiesEksisterende, propertiesNy);

                    if (propertiesNy.size() != propertiesEksisterende.size()) {
                        log.info("Task properties for tasktype '{}' matchet ikke eksisterende task: {}, nye task properties: {}, eksisterende task properties: {}", taskType, eksisterende.getId(), propertiesNy.stringPropertyNames(), propertiesEksisterende.stringPropertyNames());
                        continue;
                    }

                    boolean propsMatch = true;
                    for (String propName : propertiesNy.stringPropertyNames()) {
                        if (!Objects.equals(propertiesNy.getProperty(propName), propertiesEksisterende.getProperty(propName))) {
                            log.info("Task property '{}' matchet ikke, tasktype: '{}', eksisterende task: {}", propName, taskType, eksisterende.getId());
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
                log.info("Fant ingen matchende eksisterende task for tasktype: '{}'", taskType);
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

    private void håndterSpesialtilfelleFortsettBehandling(ProsessTaskData eksisterendeTask, Set<ProsessTaskData> eksisterendeTasks, Properties propertiesEksisterende, Properties propertiesNy) {
        if (eksisterendeTask.getTaskType().equals("behandlingskontroll.fortsettBehandling")) {
            for (ProsessTaskData annenEksisterendeTask : eksisterendeTasks) {
                if (annenEksisterendeTask.getTaskType().equals("grunnlag.diffOgReposisjoner")
                    && annenEksisterendeTask.getGruppe() != null && annenEksisterendeTask.getGruppe().equals(eksisterendeTask.getGruppe())
                    && annenEksisterendeTask.getSekvens() != null && eksisterendeTask.getSekvens() != null
                    && Long.parseLong(annenEksisterendeTask.getSekvens()) < Long.parseLong(eksisterendeTask.getSekvens())) {
                    // Ser bort ifra spesialproperties på fortsettBehandling dersom vi uansett skal kjøre diffOgReposisjoner først
                    propertiesNy.remove("gjenopptaSteg");
                    propertiesNy.remove("manuellFortsettelse");
                    propertiesEksisterende.remove("gjenopptaSteg");
                    propertiesEksisterende.remove("manuellFortsettelse");
                }
            }
        }
    }

    private String toStringTask(Collection<ProsessTaskData> tasks) {
        return tasks.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    private String toStringEntry(Collection<Entry> tasks) {
        return tasks.stream().map(Entry::getTask).map(Object::toString).collect(Collectors.joining(", "));
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
            tasks = finnAlleForAngittSøk(fagsakId, behandlingId, gruppe, new ArrayList<>(statuser), kunGruppeSekvens);
        }

        if (tasks.isEmpty()) {
            // ignorerer alle ferdig, suspendert, når vi søker blant alle grupper
            statuser.remove(ProsessTaskStatus.FERDIG);
            statuser.remove(ProsessTaskStatus.KJOERT);
            statuser.remove(ProsessTaskStatus.SUSPENDERT);
            tasks = finnAlleForAngittSøk(fagsakId, behandlingId, null, new ArrayList<>(statuser), kunGruppeSekvens);
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

        var skalSuspenderes = finnAlleForAngittSøk(fagsakId, String.valueOf(behandlingId), null, feiletStatus, false);
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

        var skalSlettes = finnAlleForAngittSøk(fagsakId, String.valueOf(behandlingId), null, uferdigStatuser, false);
        if (!skalSlettes.isEmpty()) {
            em.flush(); // flush alt annet
            em.createNativeQuery("delete from fagsak_prosess_task fpt where fpt.fagsak_id=:fagsakId and fpt.behandling_id=:behandlingId")
                .setParameter("fagsakId", fagsakId)
                .setParameter("behandlingId", String.valueOf(behandlingId))
                .executeUpdate();
            em.flush();
            for (var s : skalSlettes) {
                em.createNativeQuery("delete from prosess_task p where p.id = :pid")
                    .setParameter("pid", s.getId())
                    .executeUpdate();
                em.flush();
            }
            int frigitt = frigiVetoBlokkertAvSlettedeTasker(skalSlettes.stream().map(ProsessTaskData::getId).toList());
            log.info("Slettet {} uferdige tasker og frigitt {} tasker som var blokkert av dem, for fagsak[{}], behandling[{}]",
                skalSlettes.size(), frigitt, fagsakId, behandlingId);
        }

    }

    public boolean prosessTaskFinnes(Long prosessTaskId) {
        return (Boolean) em.createNativeQuery("select exists(select 1 from prosess_task where id = :id)")
            .setParameter("id", prosessTaskId)
            .getSingleResult();
    }

    /**
     * Henter alle koblinger for fagsaken, også foreldreløse koblinger der prosesstasken ikke lenger finnes.
     */
    public List<FagsakProsessTaskKobling> hentKoblinger(Long fagsakId) {
        String sql = """
            select fpt.id, fpt.prosess_task_id, fpt.behandling_id, fpt.task_type, fpt.gruppe_sekvensnr, fpt.opprettet_tid,
                   pt.id is not null as prosess_task_finnes, pt.status, pt.blokkert_av
            from fagsak_prosess_task fpt
            left join prosess_task pt on pt.id = fpt.prosess_task_id
            where fpt.fagsak_id = :fagsakId
            order by fpt.gruppe_sekvensnr nulls last, fpt.id
            """;

        @SuppressWarnings("unchecked")
        NativeQuery<Object[]> query = (NativeQuery<Object[]>) em.createNativeQuery(sql);
        List<Object[]> rader = query
            .setParameter("fagsakId", fagsakId)
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("prosess_task_id", StandardBasicTypes.LONG)
            .addScalar("behandling_id", StandardBasicTypes.STRING)
            .addScalar("task_type", StandardBasicTypes.STRING)
            .addScalar("gruppe_sekvensnr", StandardBasicTypes.LONG)
            .addScalar("opprettet_tid", StandardBasicTypes.LOCAL_DATE_TIME)
            .addScalar("prosess_task_finnes", StandardBasicTypes.BOOLEAN)
            .addScalar("status", StandardBasicTypes.STRING)
            .addScalar("blokkert_av", StandardBasicTypes.LONG)
            .setHint(HibernateHints.HINT_READ_ONLY, "true")
            .getResultList();

        return rader.stream()
            .map(r -> new FagsakProsessTaskKobling(
                (Long) r[0],
                (Long) r[1],
                (String) r[2],
                (String) r[3],
                (Long) r[4],
                (LocalDateTime) r[5],
                (Boolean) r[6],
                r[7] == null ? null : ProsessTaskStatus.valueOf((String) r[7]),
                (Long) r[8]))
            .toList();
    }

    /**
     * Fagsaker som har minst én kobling til en prosesstask som ikke lenger finnes.
     */
    public List<Long> finnFagsakerMedForeldreløseKoblinger() {
        return em.createNativeQuery("""
                select distinct fpt.fagsak_id
                from fagsak_prosess_task fpt
                where not exists (select 1 from prosess_task pt where pt.id = fpt.prosess_task_id)
                order by fpt.fagsak_id
                """, Long.class)
            .setHint(HibernateHints.HINT_READ_ONLY, "true")
            .getResultList();
    }

    /**
     * Fjerner koblinger til prosesstasker som ikke lenger finnes, og frigir tasker som står i VETO blokkert av dem.
     * Sletter kun rader på angitt fagsak der prosesstasken mangler. Kan ikke alle angitte koblinger fjernes, kastes
     * exception slik at transaksjonen rulles tilbake og ingenting fjernes.
     *
     * @return antall tasker som ble frigitt fra VETO
     */
    public int fjernForeldreløseKoblinger(Long fagsakId, Set<Long> prosessTaskIder) {
        Objects.requireNonNull(fagsakId, "fagsakId");
        if (prosessTaskIder == null || prosessTaskIder.isEmpty()) {
            throw new IllegalArgumentException("Må angi minst én prosessTaskId");
        }
        int fjernet = em.createNativeQuery("""
                delete from fagsak_prosess_task fpt
                where fpt.fagsak_id = :fagsakId
                  and fpt.prosess_task_id in (:prosessTaskIder)
                  and not exists (select 1 from prosess_task pt where pt.id = fpt.prosess_task_id)
                """)
            .setParameter("fagsakId", fagsakId)
            .setParameter("prosessTaskIder", prosessTaskIder)
            .executeUpdate();
        if (fjernet != prosessTaskIder.size()) {
            throw new IllegalStateException("Forventet å fjerne " + prosessTaskIder.size() + " foreldreløse koblinger for fagsak[" + fagsakId
                + "], men fant " + fjernet + ". Avbryter uten å fjerne noe.");
        }
        int frigitt = frigiVetoBlokkertAvSlettedeTasker(prosessTaskIder);
        em.flush();
        log.info("Fjernet foreldreløse koblinger for fagsak[{}] til prosesstasker {}. Frigitt {} tasker fra VETO.", fagsakId, prosessTaskIder, frigitt);
        return frigitt;
    }

    /**
     * Frigir tasker i VETO som var blokkert av tasker som er slettet. k9-prosesstask frigir bare når den blokkerende
     * tasken finnes og er ferdig, så uten dette blir de stående i VETO for alltid.
     * <p>
     * Omgår optimistisk låsing med vilje, på samme måte som k9-prosesstask gjør i frigiVeto og unblockTasks. Postgres
     * vurderer where-betingelsen på nytt etter å ha låst raden, så en task som er endret i mellomtiden (frigitt, kjørt
     * eller vetoet av en annen task) blir ikke truffet. Versjonen økes, så den som eventuelt holder entiteten, får
     * OptimisticLockException.
     * <p>
     * Kaster exception hvis noen av de blokkerende taskene fortsatt finnes, slik at metoden ikke kan brukes til å frigi
     * tasker som skal vente på en task som fortsatt kan kjøres.
     */
    private int frigiVetoBlokkertAvSlettedeTasker(Collection<Long> slettedeTaskIder) {
        if (slettedeTaskIder.isEmpty()) {
            return 0;
        }
        @SuppressWarnings("unchecked")
        List<Number> finnesFortsatt = em.createNativeQuery("select id from prosess_task where id in (:ider)")
            .setParameter("ider", slettedeTaskIder)
            .getResultList();
        if (!finnesFortsatt.isEmpty()) {
            throw new IllegalStateException("Kan bare frigi tasker blokkert av slettede tasker, men disse finnes fortsatt: " + finnesFortsatt);
        }
        return em.createNativeQuery("""
                update prosess_task set status = 'KLAR'
                  , blokkert_av = null
                  , siste_kjoering_feil_kode = null
                  , siste_kjoering_feil_tekst = null
                  , neste_kjoering_etter = null
                  , versjon = versjon + 1
                where status = 'VETO' and blokkert_av in (:ider)
                """)
            .setParameter("ider", slettedeTaskIder)
            .executeUpdate();
    }

    public List<ProsessTaskData> finnAlleÅpneTasksForAngittSøk(Long fagsakId, Long behandlingId, String gruppe) {
        Set<ProsessTaskStatus> uferdigStatuser = EnumSet.complementOf(EnumSet.of(ProsessTaskStatus.FERDIG, ProsessTaskStatus.KJOERT));
        var fom = Tid.TIDENES_BEGYNNELSE.atStartOfDay();
        var tom = Tid.TIDENES_ENDE.plusDays(1).atStartOfDay();
        return finnAlleForAngittSøk(fagsakId, behandlingId.toString(), gruppe, uferdigStatuser, true);
    }

}
