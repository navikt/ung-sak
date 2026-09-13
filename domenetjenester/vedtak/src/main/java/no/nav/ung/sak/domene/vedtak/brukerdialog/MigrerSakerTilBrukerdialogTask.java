package no.nav.ung.sak.domene.vedtak.brukerdialog;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@ProsessTask(MigrerSakerTilBrukerdialogTask.TASKTYPE)
public class MigrerSakerTilBrukerdialogTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "migrer.vedtak.brukerdialog";

    public static final String FRA_OG_MED_FAGSAK_ID = "fraOgMedFagsakId";
    public static final String ANTALL = "antall";
    public static final String DRY_RUN = "dryrun";

    private static final int STANDARD_ANTALL = 200;

    private static final Logger log = LoggerFactory.getLogger(MigrerSakerTilBrukerdialogTask.class);

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    MigrerSakerTilBrukerdialogTask() {
        // for CDI proxy
    }

    @Inject
    public MigrerSakerTilBrukerdialogTask(EntityManager entityManager,
                                          BehandlingRepository behandlingRepository,
                                          ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        long fraOgMedFagsakId = lesLong(prosessTaskData, FRA_OG_MED_FAGSAK_ID, 0L);
        int antall = (int) lesLong(prosessTaskData, ANTALL, STANDARD_ANTALL);
        boolean dryRun = Boolean.parseBoolean(prosessTaskData.getPropertyValue(DRY_RUN));

        List<Fagsak> fagsaker = hentNesteFagsaker(fraOgMedFagsakId, antall);
        if (fagsaker.isEmpty()) {
            log.info("Migrering til brukerdialog er ferdig. Ingen flere saker etter fagsakId={}.", fraOgMedFagsakId);
            return;
        }

        var publiseringstasker = fagsaker.stream()
            .map(this::opprettPubliseringstask)
            .flatMap(Optional::stream)
            .toList();

        long sisteFagsakId = fagsaker.getLast().getId();
        if (dryRun) {
            log.info("Dryrun: {} av {} saker mellom fagsakId={} og {} ville blitt publisert til brukerdialog.",
                publiseringstasker.size(), fagsaker.size(), fraOgMedFagsakId, sisteFagsakId);
            return;
        }

        if (!publiseringstasker.isEmpty()) {
            var gruppe = new ProsessTaskGruppe();
            gruppe.addNesteParallell(publiseringstasker);
            prosessTaskTjeneste.lagre(gruppe);
        }

        log.info("Publiserte {} av {} saker mellom fagsakId={} og {} til brukerdialog.",
            publiseringstasker.size(), fagsaker.size(), fraOgMedFagsakId, sisteFagsakId);

        if (fagsaker.size() == antall) {
            prosessTaskTjeneste.lagre(nesteBolk(sisteFagsakId, antall));
        }
    }

    private List<Fagsak> hentNesteFagsaker(long fraOgMedFagsakId, int antall) {
        TypedQuery<Fagsak> query = entityManager.createQuery(
            "SELECT f FROM Fagsak f WHERE f.ytelseType = :ytelseType AND f.id > :fagsakId ORDER BY f.id",
            Fagsak.class);
        query.setParameter("ytelseType", FagsakYtelseType.AKTIVITETSPENGER);
        query.setParameter("fagsakId", fraOgMedFagsakId);
        query.setMaxResults(antall);
        return query.getResultList();
    }

    private Optional<ProsessTaskData> opprettPubliseringstask(Fagsak fagsak) {
        Optional<Behandling> behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsak.getId());
        if (behandling.isEmpty()) {
            log.info("Hopper over saksnummer={} - ingen avsluttet behandling å melde inn.", fagsak.getSaksnummer());
            return Optional.empty();
        }

        var taskData = ProsessTaskData.forProsessTask(PubliserSakTilBrukerdialogTask.class);
        taskData.setBehandling(fagsak.getId(), behandling.get().getId(), fagsak.getAktørId().getId());
        taskData.setCallIdFraEksisterende();
        return Optional.of(taskData);
    }

    private ProsessTaskData nesteBolk(long sisteFagsakId, int antall) {
        var taskData = ProsessTaskData.forProsessTask(MigrerSakerTilBrukerdialogTask.class);
        taskData.setProperty(FRA_OG_MED_FAGSAK_ID, Long.toString(sisteFagsakId));
        taskData.setProperty(ANTALL, Integer.toString(antall));
        taskData.setCallIdFraEksisterende();
        return taskData;
    }

    private static long lesLong(ProsessTaskData prosessTaskData, String key, long standardverdi) {
        String verdi = prosessTaskData.getPropertyValue(key);
        return verdi == null || verdi.isBlank() ? standardverdi : Long.parseLong(verdi);
    }
}
