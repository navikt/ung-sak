package no.nav.ung.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.ung.sak.web.app.tjenester.forvaltning.DumpOutput;

@ApplicationScoped
@RapportTypeRef(RapportType.UTBETALING_PER_DAG)
public class UttrekkUtbetalingPerDag implements RapportGenerator {

    private EntityManager entityManager;

    UttrekkUtbetalingPerDag() {
        //
    }

    @Inject
    public UttrekkUtbetalingPerDag(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DumpOutput> generer(FagsakYtelseType ytelseType, DatoIntervallEntitet periode) {
        String sql = """
                SELECT cast(t.dag as date), t.kjoenn, round(SUM(t.utbetaling), 2) as utbetaling, round(SUM(t.dagsekvivalenter), 2) AS dagsekvivalenter, SUM(t.antalletUlikeKrav) AS antalletUlikeKrav, COUNT(*) AS fagsakDager
                FROM (
                  SELECT s.saksnummer,
                         s.dag,
                         SUM(s.utbetaling) as utbetaling,
                         SUM(dagsekvivalenter) AS dagsekvivalenter,
                         kjoenn,
                         COUNT(*) antalletUlikeKrav
                  FROM (
                    SELECT beregning.saksnummer,
                      generate_series(beregning.fom, beregning.tom, '1 day') as dag,
                      beregning.dagsats * (beregning.utbetalingsgrad / 100.0) as utbetaling,
                      (beregning.utbetalingsgrad / 100.0) as dagsekvivalenter,
                      kjoenn
                    FROM (
                      select f.saksnummer,
                        bp.br_periode_fom as fom,
                        bp.br_periode_tom as tom,
                        ba.dagsats,
                        ba.utbetalingsgrad,
                           (select distinct bruker_kjoenn from po_personopplysning po where po.aktoer_id=f.bruker_aktoer_id) as kjoenn
                      from br_resultat_behandling br
                               Inner join br_beregningsresultat res on br.bg_beregningsresultat_fp_id=res.id
                               Inner join behandling b on b.id=br.behandling_id
                               Inner join fagsak f on f.id=b.fagsak_id
                               Inner join br_periode bp on bp.beregningsresultat_fp_id=res.id
                               Inner join br_andel ba on ba.br_periode_id=bp.id
                      where br.aktiv=true
                        AND f.ytelse_type= :ytelseType
                        AND b.behandling_status = 'AVSLU'
                        AND bp.br_periode_fom <= :tom AND bp.br_periode_tom >= :fom
                        AND NOT EXISTS (
                          SELECT *
                          FROM Behandling b2
                          WHERE b2.id != b.id
                            AND b2.opprettet_dato > b.opprettet_dato
                            AND b2.fagsak_id = b.fagsak_id
                            AND b2.behandling_status = 'AVSLU'
                        )
                    ) beregning
                  ) s
                  where s.dag >= :fom and s.dag <= :tom
                  GROUP BY s.saksnummer, s.dag, s.kjoenn
                ) t
                GROUP by t.dag, t.kjoenn
                ORDER BY t.dag, t.kjoenn
                """;

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("ytelseType", ytelseType.getKode())
            .setParameter("fom", periode.getFomDato())
            .setParameter("tom", periode.getTomDato()) // tar alt overlappende
            .setHint("jakarta.persistence.query.timeout", 1 * 90 * 1000) // 1:30 min
        ;
        String path = "utbetaling-per-dag.csv";

        try (Stream<Tuple> stream = query.getResultStream()) {
            return CsvOutput.dumpResultSetToCsv(path, stream)
                .map(v -> List.of(v)).orElse(List.of());
        }

    }

}
