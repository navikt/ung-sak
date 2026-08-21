package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * One-shot batch-task som populerer periode_maks_dato for alle aktive grunnlag.
 *
 * <p>Del 1: Oppdaterer eksisterende maks_periode-rader som mangler periode_maks_dato.
 * <p>Del 2: Oppretter nye maks_periode-rader for grunnlag som mangler kobling helt.
 *
 * <p>Pr. nå har ingen saker forlenget periode, men tasken respekterer har_forlenget_periode-flagget
 * og beregner riktig antall virkedager (260 normal, 300 forlenget).
 *
 * <p>Tasken er idempotent og kan kjøres på nytt uten sideeffekter.
 */
@ApplicationScoped
@ProsessTask(value = BackfillPeriodeMaksDatoTask.TASKTYPE, maxFailedRuns = 1)
public class BackfillPeriodeMaksDatoTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.backfillPeriodeMaksDato";

    private static final Logger log = LoggerFactory.getLogger(BackfillPeriodeMaksDatoTask.class);

    private EntityManager entityManager;

    public BackfillPeriodeMaksDatoTask() {
        // CDI
    }

    @Inject
    public BackfillPeriodeMaksDatoTask(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        backfillEksisterendeMaksPeriodeRader();
        opprettManglendeMaksPeriodeRader();
        entityManager.flush();
    }

    /**
     * Oppdaterer eksisterende maks_periode-rader som mangler periode_maks_dato.
     */
    private void backfillEksisterendeMaksPeriodeRader() {
        @SuppressWarnings("unchecked")
        List<Object[]> rader = entityManager.createNativeQuery(
                "select mp.id, mp.har_forlenget_periode, min(up.fom) as startdato " +
                    "from ung_ungdomsprogram_maks_periode mp " +
                    "join ung_gr_ungdomsprogramperiode gr on gr.ung_ungdomsprogram_maks_periode_id = mp.id " +
                    "join ung_ungdomsprogramperioder ups on ups.id = gr.ung_ungdomsprogramperioder_id " +
                    "join ung_ungdomsprogramperiode up on up.ung_ungdomsprogramperioder_id = ups.id " +
                    "where mp.periode_maks_dato is null " +
                    "group by mp.id, mp.har_forlenget_periode")
            .getResultList();

        log.info("Fant {} eksisterende maks_periode-rader uten periode_maks_dato", rader.size());

        int oppdatert = 0;
        for (Object[] rad : rader) {
            Long maksPeriodeId = ((Number) rad[0]).longValue();
            boolean harForlengetPeriode = (Boolean) rad[1];
            LocalDate startdato = (LocalDate) rad[2];

            LocalDate maksDato = justerTilSisteVirkedag(FagsakperiodeUtleder.finnTomDato(
                startdato,
                new LocalDateTimeline<>(startdato, startdato, true),
                harForlengetPeriode));

            entityManager.createNativeQuery(
                    "update ung_ungdomsprogram_maks_periode set periode_maks_dato = :maksDato where id = :id")
                .setParameter("maksDato", maksDato)
                .setParameter("id", maksPeriodeId)
                .executeUpdate();

            oppdatert++;
        }

        log.info("Oppdatert periode_maks_dato for {} eksisterende maks_periode-rader", oppdatert);
    }

    /**
     * Oppretter nye maks_periode-rader for aktive grunnlag som mangler kobling.
     */
    private void opprettManglendeMaksPeriodeRader() {
        @SuppressWarnings("unchecked")
        List<Object[]> rader = entityManager.createNativeQuery(
                "select gr.id, min(up.fom) as startdato " +
                    "from ung_gr_ungdomsprogramperiode gr " +
                    "join ung_ungdomsprogramperioder ups on ups.id = gr.ung_ungdomsprogramperioder_id " +
                    "join ung_ungdomsprogramperiode up on up.ung_ungdomsprogramperioder_id = ups.id " +
                    "where gr.ung_ungdomsprogram_maks_periode_id is null " +
                    "and gr.aktiv = true " +
                    "group by gr.id")
            .getResultList();

        log.info("Fant {} aktive grunnlag uten maks_periode-rad", rader.size());

        int oppdatert = 0;
        for (Object[] rad : rader) {
            Long grunnlagId = ((Number) rad[0]).longValue();
            LocalDate startdato = (LocalDate) rad[1];

            LocalDate maksDato = justerTilSisteVirkedag(FagsakperiodeUtleder.finnTomDato(
                startdato,
                new LocalDateTimeline<>(startdato, startdato, true),
                false));

            Long maksPeriodeId = ((Number) entityManager.createNativeQuery(
                    "insert into ung_ungdomsprogram_maks_periode (id, har_forlenget_periode, periode_maks_dato, hjemmel) " +
                        "values (nextval('seq_ung_ungdomsprogram_maks_periode_id'), false, :maksDato, 'UNG_FRSKRFT_6') " +
                        "returning id")
                .setParameter("maksDato", maksDato)
                .getSingleResult()).longValue();

            entityManager.createNativeQuery(
                    "update ung_gr_ungdomsprogramperiode set ung_ungdomsprogram_maks_periode_id = :maksPeriodeId where id = :grunnlagId")
                .setParameter("maksPeriodeId", maksPeriodeId)
                .setParameter("grunnlagId", grunnlagId)
                .executeUpdate();

            oppdatert++;
        }

        log.info("Opprettet maks_periode for {} grunnlag", oppdatert);
    }

    private static LocalDate justerTilSisteVirkedag(LocalDate dato) {
        return switch (dato.getDayOfWeek()) {
            case SATURDAY -> dato.minusDays(1);
            case SUNDAY -> dato.minusDays(2);
            default -> dato;
        };
    }
}
