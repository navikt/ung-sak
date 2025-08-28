package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.metrikker.bigquery.tabeller.ungdomsprogram.DagerIProgrammetRecord;
import org.hibernate.query.NativeQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Repository for beregning av statistikk over antall dager i ungdomsprogrammet.
 * <p>
 * Denne klassen håndterer beregning av hvor mange virkedager (mandag til fredag)
 * ungdomsytelse-mottakere har vært i ungdomsprogrammet. Helger (lørdag og søndag)
 * ekskluderes fra beregningen.
 * <p>
 * Statistikken brukes til rapportering til BigQuery for analyse av programvarighet
 * og effektivitet av ungdomsprogrammet.
 *
 * @author NAV
 * @since 1.0.0
 */
@Dependent
public class AntallDagerStatistikk {

    /** Kode for utdaterte ytelse-typer som skal ekskluderes fra statistikken */
    private static final String OBSOLETE_KODE = FagsakYtelseType.OBSOLETE.getKode();

    /** EntityManager for databasetilgang */
    private final EntityManager entityManager;

    /**
     * Konstruktør for AntallDagerStatistikk.
     *
     * @param entityManager EntityManager for databaseoperasjoner
     */
    @Inject
    public AntallDagerStatistikk(
        EntityManager entityManager
    ) {
        this.entityManager = entityManager;
    }

    /**
     * Henter statistikk over antall dager i programmet basert på dagens dato.
     * <p>
     * Dette er hovedmetoden som brukes i produksjon. Den kaller den parameteriserte
     * versjonen med dagens dato som referansepunkt.
     *
     * @return Collection av DagerIProgrammetRecord med statistikk
     */
    Collection<DagerIProgrammetRecord> dagerIProgrammet() {
        return dagerIProgrammet(LocalDate.now());
    }

    /**
     * Henter statistikk over antall virkedager i ungdomsprogrammet.
     * <p>
     * Denne metoden beregner hvor mange virkedager (mandag-fredag) hver deltaker
     * har vært aktiv i ungdomsprogrammet, frem til den spesifiserte datoen.
     * Helger (lørdag og søndag) ekskluderes fra beregningen.
     * <p>
     * Funksjonelt:
     * - Kun virkedager telles (mandag til fredag)
     * - Beregning stopper ved den spesifiserte datoen minus 1 dag
     * - Støtter flere perioder per fagsak
     * - Kun siste behandling per fagsak brukes
     * <p>
     * Teknisk:
     * - Bruker PostgreSQL sin generate_series for å iterere gjennom datoer
     * - EXTRACT(DOW FROM d) IN (1,2,3,4,5) filtrerer kun ukedager
     * - CTE (Common Table Expression) for strukturert beregning
     * - Summerer opp alle perioder per fagsak før gruppering
     *
     * @param dagensDato Referansedato for beregningen (typisk dagens dato)
     * @return Collection av DagerIProgrammetRecord med statistikk gruppert etter antall dager
     */
    Collection<DagerIProgrammetRecord> dagerIProgrammet(LocalDate dagensDato) {

        String sql = """
            WITH periode_dager AS (
                -- Første CTE: Beregn arbeidsdager for hver enkelt periode
                SELECT f.id as fagsak_id,
                       periode.fom,
                       LEAST(periode.tom, CAST(:dagensDato AS date) - INTERVAL '1 day') as tom_adjusted,
                       (SELECT COUNT(*)
                        FROM generate_series(
                            periode.fom::date,
                            LEAST(periode.tom, CAST(:dagensDato AS date) - INTERVAL '1 day')::date,
                            '1 day'::interval
                        ) AS d
                        WHERE EXTRACT(DOW FROM d) IN (1, 2, 3, 4, 5)  -- Kun mandag(1) til fredag(5)
                       ) as weekdays
                FROM fagsak f
                INNER JOIN behandling b ON b.fagsak_id = f.id
                INNER JOIN UNG_GR_UNGDOMSPROGRAMPERIODE gr ON gr.behandling_id = b.id
                INNER JOIN UNG_UNGDOMSPROGRAMPERIODER perioder ON perioder.id = gr.ung_ungdomsprogramperioder_id
                INNER JOIN UNG_UNGDOMSPROGRAMPERIODE periode ON periode.ung_ungdomsprogramperioder_id = perioder.id
                WHERE f.ytelse_type <> :obsoleteKode  -- Ekskluder utdaterte ytelse-typer
                  AND gr.aktiv = true                 -- Kun aktive grunnlag
                  AND b.opprettet_tid = (SELECT max(b2.opprettet_tid) FROM behandling b2 WHERE b2.fagsak_id = f.id)  -- Siste behandling
                  AND periode.fom <= CAST(:dagensDato AS date) - INTERVAL '1 day'  -- Periode har startet
            ),
            fagsak_totaler AS (
                -- Andre CTE: Summer opp alle arbeidsdager per fagsak
                SELECT fagsak_id, SUM(weekdays) as total_weekdays
                FROM periode_dager
                WHERE weekdays > 0  -- Kun perioder med faktiske arbeidsdager
                GROUP BY fagsak_id
            )
            -- Hovedspørring: Grupper fagsaker etter totalt antall arbeidsdager
            SELECT total_weekdays as dager_i_programmet, COUNT(*) as antall
            FROM fagsak_totaler
            WHERE total_weekdays > 0  -- Ekskluder fagsaker uten arbeidsdager
            GROUP BY total_weekdays
            ORDER BY total_weekdays
            """;

        // Opprett og kjør SQL-spørringen
        NativeQuery<Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<Tuple> stream = query
            .setParameter("obsoleteKode", OBSOLETE_KODE)
            .setParameter("dagensDato", dagensDato)
            .getResultStream();

        // Konverter resultatet til DagerIProgrammetRecord objekter
        return stream
            .filter(t -> t.get(0, BigDecimal.class).compareTo(BigDecimal.ZERO) > 0) // Ekstra sikkerhet: filtrer bort 0-verdier
            .map(t -> {
                BigDecimal antallDager = t.get(0, BigDecimal.class);
                Long antall = t.get(1, Long.class);

                // Opprett record med antall fagsaker, antall dager og tidsstempel
                return new DagerIProgrammetRecord(BigDecimal.valueOf(antall), antallDager.longValue(), ZonedDateTime.now());
            }).toList();
    }

}
