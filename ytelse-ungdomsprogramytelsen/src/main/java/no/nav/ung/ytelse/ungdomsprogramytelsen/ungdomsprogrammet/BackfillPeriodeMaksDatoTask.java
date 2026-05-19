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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * One-shot batch-task som oppretter ung_ungdomsprogram_maks_periode for alle aktive grunnlag
 * som mangler en slik rad.
 *
 * <p>Pr. nå har ingen saker forlenget periode, så alle settes til har_forlenget_periode=false
 * og periode_maks_dato = startdato + 260 virkedager (justert til fredag hvis helg).
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
        // Finner alle aktive grunnlag som mangler maks_periode-rad, sammen med tidligste startdato
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

            // Ingen saker har forlenget periode, beregner startdato + 260 virkedager
            // Justerer til fredag hvis beregningen havner i helg
            LocalDate maksDato = justerTilSisteVirkedag(FagsakperiodeUtleder.finnTomDato(
                startdato,
                new LocalDateTimeline<>(startdato, startdato, true),
                false));

            // Opprett ny maks_periode-rad
            Long maksPeriodeId = ((Number) entityManager.createNativeQuery(
                    "insert into ung_ungdomsprogram_maks_periode (id, har_forlenget_periode, periode_maks_dato, hjemmel) " +
                        "values (nextval('seq_ung_ungdomsprogram_maks_periode_id'), false, :maksDato, 'UNG_FRSKRFT_6') " +
                        "returning id")
                .setParameter("maksDato", maksDato)
                .getSingleResult()).longValue();

            // Koble grunnlaget til maks_periode-raden
            entityManager.createNativeQuery(
                    "update ung_gr_ungdomsprogramperiode set ung_ungdomsprogram_maks_periode_id = :maksPeriodeId where id = :grunnlagId")
                .setParameter("maksPeriodeId", maksPeriodeId)
                .setParameter("grunnlagId", grunnlagId)
                .executeUpdate();

            oppdatert++;
        }

        log.info("Opprettet maks_periode for {} grunnlag", oppdatert);
        entityManager.flush();
    }

    private static LocalDate justerTilSisteVirkedag(LocalDate dato) {
        return switch (dato.getDayOfWeek()) {
            case SATURDAY -> dato.minusDays(1);
            case SUNDAY -> dato.minusDays(2);
            default -> dato;
        };
    }
}
