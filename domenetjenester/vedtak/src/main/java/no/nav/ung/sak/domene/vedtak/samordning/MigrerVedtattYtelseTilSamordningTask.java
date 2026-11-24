package no.nav.ung.sak.domene.vedtak.samordning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.samordning.VedtakYtelseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Engangsmigrering som lagrer vedtak for samordning for eksisterende saker.
 * <p>
 * Oppretter én {@link LagreVedtattYtelseTask} per fagsak, for siste iverksatte, ikke henlagte ytelsesbehandling.
 * Oppslag bruker bare aktivt vedtak, så eldre vedtak trengs ikke. Vedtak som allerede er lagret, hoppes over, og vedtak som
 * ikke er ferdig iverksatt, lagres av {@code VedtakFattetEventObserver} når iverksettingen er ferdig. Lagringstaskene
 * opprettes i én gruppe og kjøres parallelt. Kan kjøres på nytt.
 * <p>
 * Dryrun logger antall saker uten å opprette lagringstasker.
 */
@ApplicationScoped
@ProsessTask(MigrerVedtattYtelseTilSamordningTask.TASKTYPE)
public class MigrerVedtattYtelseTilSamordningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "migrer.vedtak.samordning";
    public static final String DRY_RUN = "dryrun";

    static final Set<FagsakYtelseType> YTELSER = Set.of(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);

    private static final Logger log = LoggerFactory.getLogger(MigrerVedtattYtelseTilSamordningTask.class);

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private VedtakYtelseRepository vedtakYtelseRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    MigrerVedtattYtelseTilSamordningTask() {
        // for CDI proxy
    }

    @Inject
    public MigrerVedtattYtelseTilSamordningTask(EntityManager entityManager,
                                                BehandlingRepository behandlingRepository,
                                                BehandlingVedtakRepository behandlingVedtakRepository,
                                                VedtakYtelseRepository vedtakYtelseRepository,
                                                ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.vedtakYtelseRepository = vedtakYtelseRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        boolean dryRun = Boolean.parseBoolean(prosessTaskData.getPropertyValue(DRY_RUN));

        List<Fagsak> fagsaker = hentFagsaker();
        Set<UUID> lagredeVedtak = vedtakYtelseRepository.hentVedtakReferanser(YTELSER);
        List<ProsessTaskData> lagringstasker = fagsaker.stream()
            .flatMap(fagsak -> finnSisteIverksatteBehandling(fagsak)
                .filter(behandling -> !lagredeVedtak.contains(behandling.getUuid()))
                .map(behandling -> lagringstask(fagsak, behandling))
                .stream())
            .toList();

        if (dryRun) {
            log.info("Dryrun: {} av {} saker mangler lagret vedtak for samordning.", lagringstasker.size(), fagsaker.size());
            return;
        }
        if (!lagringstasker.isEmpty()) {
            prosessTaskTjeneste.lagre(new ProsessTaskGruppe().addNesteParallell(lagringstasker));
        }
        log.info("Opprettet lagring av vedtak for samordning for {} av {} saker.", lagringstasker.size(), fagsaker.size());
    }

    private List<Fagsak> hentFagsaker() {
        return entityManager.createQuery("SELECT f FROM Fagsak f WHERE f.ytelseType IN (:ytelseTyper) ORDER BY f.id", Fagsak.class)
            .setParameter("ytelseTyper", YTELSER)
            .getResultList();
    }

    // Sortert med nyeste vedtak først. Utvalget tar også med IVERKSETTER_VEDTAK, der vedtaket ennå ikke er iverksatt.
    private Optional<Behandling> finnSisteIverksatteBehandling(Fagsak fagsak) {
        return behandlingRepository.finnAlleAvsluttedeIkkeHenlagteYtelsebehandlinger(fagsak.getId()).stream()
            .filter(this::erIverksatt)
            .findFirst();
    }

    private boolean erIverksatt(Behandling behandling) {
        return behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId())
            .map(BehandlingVedtak::getIverksettingStatus)
            .filter(IverksettingStatus.IVERKSATT::equals)
            .isPresent();
    }

    private static ProsessTaskData lagringstask(Fagsak fagsak, Behandling behandling) {
        var taskData = ProsessTaskData.forProsessTask(LagreVedtattYtelseTask.class);
        taskData.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());
        taskData.setCallIdFraEksisterende();
        return taskData;
    }
}
