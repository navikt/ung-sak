package no.nav.ung.sak.domene.vedtak.samordning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Engangsmigrering som lagrer vedtak for samordning for eksisterende saker.
 * <p>
 * Oppretter én {@link LagreVedtattYtelseTask} per avsluttet, ikke henlagt ytelsesbehandling, i kronologisk rekkefølge per fagsak.
 * Behandlingene i én fagsak kjøres sekvensielt for å unngå samtidige skrivinger på samme sak.
 * Fagsakene behandles i bolker, og hver bolk oppretter neste bolk. Kan kjøres på nytt, siden lagringen er idempotent.
 * <p>
 * Dryrun går gjennom alle bolkene uten å opprette lagringstasker, og logger totalt antall vedtak til slutt.
 */
@ApplicationScoped
@ProsessTask(MigrerVedtattYtelseTilSamordningTask.TASKTYPE)
public class MigrerVedtattYtelseTilSamordningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "migrer.vedtak.samordning";

    /** Bare fagsaker med høyere id enn denne tas med. */
    public static final String ETTER_FAGSAK_ID = "etterFagsakId";
    public static final String ANTALL = "antall";
    public static final String DRY_RUN = "dryrun";

    // Summeres fra bolk til bolk, slik at siste bolk kan logge totalen
    static final String TOTALT_VEDTAK = "totaltVedtak";
    static final String TOTALT_SAKER = "totaltSaker";

    static final Set<FagsakYtelseType> YTELSER = Set.of(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);

    private static final int STANDARD_ANTALL = 200;

    private static final Logger log = LoggerFactory.getLogger(MigrerVedtattYtelseTilSamordningTask.class);

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    MigrerVedtattYtelseTilSamordningTask() {
        // for CDI proxy
    }

    @Inject
    public MigrerVedtattYtelseTilSamordningTask(EntityManager entityManager,
                                                BehandlingRepository behandlingRepository,
                                                ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        long etterFagsakId = lesLong(prosessTaskData, ETTER_FAGSAK_ID, 0L);
        int antall = (int) lesLong(prosessTaskData, ANTALL, STANDARD_ANTALL);
        boolean dryRun = Boolean.parseBoolean(prosessTaskData.getPropertyValue(DRY_RUN));

        List<Fagsak> fagsaker = hentNesteFagsaker(etterFagsakId, antall);
        var grupper = fagsaker.stream()
            .map(this::opprettLagringstasker)
            .flatMap(Optional::stream)
            .toList();
        int antallVedtak = grupper.stream().mapToInt(g -> g.getTasks().size()).sum();
        long totaltVedtak = lesLong(prosessTaskData, TOTALT_VEDTAK, 0L) + antallVedtak;
        long totaltSaker = lesLong(prosessTaskData, TOTALT_SAKER, 0L) + grupper.size();

        if (!dryRun) {
            grupper.forEach(prosessTaskTjeneste::lagre);
        }
        String prefiks = dryRun ? "Dryrun: " : "";

        if (!fagsaker.isEmpty()) {
            log.info("{}{} vedtak fra {} av {} saker etter fagsakId={} til og med {} for samordning.",
                prefiks, antallVedtak, grupper.size(), fagsaker.size(), etterFagsakId, fagsaker.getLast().getId());
        }

        if (!fagsaker.isEmpty() && fagsaker.size() == antall) {
            prosessTaskTjeneste.lagre(nesteBolk(fagsaker.getLast().getId(), antall, dryRun, totaltVedtak, totaltSaker));
            return;
        }
        log.info("{}Migrering av vedtak til samordning er ferdig. Totalt {} vedtak fra {} saker.", prefiks, totaltVedtak, totaltSaker);
    }

    private List<Fagsak> hentNesteFagsaker(long etterFagsakId, int antall) {
        TypedQuery<Fagsak> query = entityManager.createQuery(
            "SELECT f FROM Fagsak f WHERE f.ytelseType IN (:ytelseTyper) AND f.id > :fagsakId ORDER BY f.id",
            Fagsak.class);
        query.setParameter("ytelseTyper", YTELSER);
        query.setParameter("fagsakId", etterFagsakId);
        query.setMaxResults(antall);
        return query.getResultList();
    }

    private Optional<ProsessTaskGruppe> opprettLagringstasker(Fagsak fagsak) {
        // Sortert nyeste først; snus slik at eldste vedtak lagres først
        List<Behandling> behandlinger = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteYtelsebehandlinger(fagsak.getId()).reversed();
        if (behandlinger.isEmpty()) {
            return Optional.empty();
        }

        var gruppe = new ProsessTaskGruppe();
        behandlinger.forEach(behandling -> gruppe.addNesteSekvensiell(lagringstask(fagsak, behandling)));
        return Optional.of(gruppe);
    }

    private static ProsessTaskData lagringstask(Fagsak fagsak, Behandling behandling) {
        var taskData = ProsessTaskData.forProsessTask(LagreVedtattYtelseTask.class);
        taskData.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());
        taskData.setCallIdFraEksisterende();
        return taskData;
    }

    private static ProsessTaskData nesteBolk(long sisteFagsakId, int antall, boolean dryRun, long totaltVedtak, long totaltSaker) {
        var taskData = ProsessTaskData.forProsessTask(MigrerVedtattYtelseTilSamordningTask.class);
        taskData.setProperty(ETTER_FAGSAK_ID, Long.toString(sisteFagsakId));
        taskData.setProperty(ANTALL, Integer.toString(antall));
        taskData.setProperty(DRY_RUN, Boolean.toString(dryRun));
        taskData.setProperty(TOTALT_VEDTAK, Long.toString(totaltVedtak));
        taskData.setProperty(TOTALT_SAKER, Long.toString(totaltSaker));
        taskData.setCallIdFraEksisterende();
        return taskData;
    }

    private static long lesLong(ProsessTaskData prosessTaskData, String key, long standardverdi) {
        String verdi = prosessTaskData.getPropertyValue(key);
        return verdi == null || verdi.isBlank() ? standardverdi : Long.parseLong(verdi);
    }
}
