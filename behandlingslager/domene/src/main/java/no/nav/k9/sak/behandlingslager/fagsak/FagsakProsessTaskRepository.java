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
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskEvent;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe.Entry;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEntitet;
import no.nav.vedtak.konfig.Tid;

/** Repository for å håndtere kobling mellom Fagsak (og Behandling) mot Prosess Tasks. */
@ApplicationScoped
public class FagsakProsessTaskRepository {

    private static final Logger log = LoggerFactory.getLogger(FagsakProsessTaskRepository.class);

    private EntityManager em;
    private ProsessTaskRepository prosessTaskRepository;

    private final Set<ProsessTaskStatus> ferdigStatuser = Set.of(ProsessTaskStatus.FERDIG, ProsessTaskStatus.KJOERT);

    FagsakProsessTaskRepository() {
        // for proxy
    }

    @Inject
    public FagsakProsessTaskRepository(EntityManager entityManager, ProsessTaskRepository prosessTaskRepository) {
        this.em = entityManager;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void lagre(FagsakProsessTask fagsakProsessTask) {
        ProsessTaskData ptData = prosessTaskRepository.finn(fagsakProsessTask.getProsessTaskId());
        log.debug("Linker fagsak[{}] -> prosesstask[{}], tasktype=[{}] gruppeSekvensNr=[{}]", fagsakProsessTask.getFagsakId(), fagsakProsessTask.getProsessTaskId(), ptData.getTaskType(),
            fagsakProsessTask.getGruppeSekvensNr());
        EntityManager em = getEntityManager();
        em.persist(fagsakProsessTask);
        em.flush();
    }

    public Optional<FagsakProsessTask> hent(Long prosessTaskId, boolean lås) {
        TypedQuery<FagsakProsessTask> query = getEntityManager().createQuery("from FagsakProsessTask fpt where fpt.prosessTaskId=:prosessTaskId",
            FagsakProsessTask.class);
        query.setParameter("prosessTaskId", prosessTaskId);
        if (lås) {
            query.setLockMode(LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void fjern(Long fagsakId, Long prosessTaskId, Long gruppeSekvensNr) {
        ProsessTaskData ptData = prosessTaskRepository.finn(prosessTaskId);
        log.debug("Fjerner link fagsak[{}] -> prosesstask[{}], tasktype=[{}] gruppeSekvensNr=[{}]", fagsakId, prosessTaskId, ptData.getTaskType(), gruppeSekvensNr);
        EntityManager em = getEntityManager();
        Query query = em.createNativeQuery("delete from FAGSAK_PROSESS_TASK where prosess_task_id = :prosessTaskId and fagsak_id=:fagsakId");
        query.setParameter("prosessTaskId", prosessTaskId); // NOSONAR
        query.setParameter("fagsakId", fagsakId); // NOSONAR
        query.executeUpdate();
        em.flush();
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
            .setParameter("gruppe", gruppeId, StringType.INSTANCE)
            .setParameter("nesteKjoeringFraOgMed", nesteKjoeringFraOgMed) // max oppløsning på neste_kjoering_etter er sekunder
            .setParameter("nesteKjoeringTilOgMed", nesteKjoeringTilOgMed)
            .setParameter("fagsakId", fagsakId) // NOSONAR
            .setParameter("behandlingId", behandlingId, StringType.INSTANCE) // NOSONAR
            .setHint(QueryHints.HINT_READONLY, "true");

        List<ProsessTaskEntitet> resultList = query.getResultList();
        return tilProsessTask(resultList);
    }

    public String lagreNyGruppe(ProsessTaskData taskData) {
        Optional.ofNullable(MDC.get("prosess_task_id")).ifPresent(v -> taskData.setProperty("parent.task_id", v));
        Optional.ofNullable(MDC.get("prosess_task")).ifPresent(v -> taskData.setProperty("parent.task", v));
        Optional.ofNullable(MDC.get("prosess_steg")).ifPresent(v -> taskData.setProperty("parent.steg", v));
        return prosessTaskRepository.lagre(taskData);
    }

    public String lagreNyGruppe(ProsessTaskGruppe gruppe) {
        Optional.ofNullable(MDC.get("prosess_task_id")).ifPresent(v -> gruppe.setProperty("parent.task_id", v));
        Optional.ofNullable(MDC.get("prosess_task")).ifPresent(v -> gruppe.setProperty("parent.task", v));
        Optional.ofNullable(MDC.get("prosess_steg")).ifPresent(v -> gruppe.setProperty("parent.steg", v));
        return prosessTaskRepository.lagre(gruppe);
    }

    public String lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(Long fagsakId, Long behandlingId, ProsessTaskGruppe gruppe) {
        return lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId.toString(), gruppe);
    }

    public String lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(Long fagsakId, String behandlingId, ProsessTaskGruppe gruppe) {
        // oppretter nye tasks hvis gamle har feilet og matcher angitt gruppe, eller tidligere er FERDIG. Ignorerer hvis tidligere gruppe fortsatt
        // er KLAR
        List<Entry> nyeTasks = gruppe.getTasks();
        List<ProsessTaskData> eksisterendeTasks = sjekkStatusProsessTasks(fagsakId, behandlingId, null);

        List<ProsessTaskData> matchedTasks = eksisterendeTasks;

        gruppe.setCallIdFraEksisterende();

        if (matchedTasks.isEmpty()) {
            // legg inn nye
            return lagreNyGruppe(gruppe);
        } else {

            // hvis noen er FEILET så oppretter vi ikke nye
            Optional<ProsessTaskData> feilet = matchedTasks.stream().filter(t -> t.getStatus().equals(ProsessTaskStatus.FEILET)).findFirst();

            Set<String> nyeTaskTyper = nyeTasks.stream().map(t -> t.getTask().getTaskType()).collect(Collectors.toSet());
            Set<String> eksisterendeTaskTyper = eksisterendeTasks.stream().map(t -> t.getTaskType()).collect(Collectors.toSet());

            if (!feilet.isPresent()) {
                var rest = new HashSet<>(eksisterendeTaskTyper);
                rest.retainAll(nyeTaskTyper);

                if (!rest.isEmpty()) {
                    throw new IllegalStateException("Kan ikke opprette gruppe med tasks: [" + toStringEntry(gruppe.getTasks()) + "].  Har allerede [" + toStringTask(eksisterendeTasks) + "]");
                } else {
                    return lagreNyGruppe(gruppe);
                }
            } else {
                throw new IllegalStateException("Kan ikke opprette gruppe med tasks: [" + toStringEntry(gruppe.getTasks()) + "].  Har allerede feilende task [" + feilet.get() + "]");
            }
        }

    }

    private String toStringTask(Collection<ProsessTaskData> tasks) {
        return tasks.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    private String toStringEntry(Collection<Entry> tasks) {
        return tasks.stream().map(t -> t.getTask()).map(Object::toString).collect(Collectors.joining(", "));
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
            TypedQuery<FagsakProsessTask> query = getEntityManager().createQuery("from FagsakProsessTask fpt " +
                "where fpt.fagsakId=:fagsakId and gruppeSekvensNr is not null " +
                "order by gruppeSekvensNr ",
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

    /** Observerer og vedlikeholder relasjon mellom fagsak og prosess task for enklere søk (dvs. fjerner relasjon når FERDIG). */
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

    /** Sett feilet prosesstasks som er koblet til fagsak+behandling til suspendert. */
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

    /** Sletter prosesstasks som er koblet til fagsak+behandling og er ikke kjørt. */
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
