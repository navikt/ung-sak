package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;

@ApplicationScoped
@RapportTypeRef(RapportType.UTBETALING_PER_BRUKER)
public class UttrekkUtbetalingPerBruker implements RapportGenerator {

    private EntityManager entityManager;

    UttrekkUtbetalingPerBruker() {
        //
    }

    @Inject
    public UttrekkUtbetalingPerBruker(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DumpOutput> generer(FagsakYtelseType ytelseType, DatoIntervallEntitet periode) {
        String sql = """
                 select f.saksnummer,
                      f.bruker_aktoer_id,
                      a.ident,
                      a.ident_type,
                      replace(cast(b.endret_tid as varchar), ' ', 'T') behandling_vedtatt_tid,
                      bp.br_periode_fom as fom,
                      bp.br_periode_tom as tom,
                      ba.dagsats,
                      ba.utbetalingsgrad,
                      round(ba.dagsats * (ba.utbetalingsgrad / 100.0), 0) as utbetaling,
                      round(ba.utbetalingsgrad / 100.0, 2) as dagsekvivalenter,
                      ba.feriepenger_beloep,
                      ba.arbeidsgiver_orgnr,
                      dense_rank() over (partition by saksnummer, arbeidsgiver_orgnr, arbeidsgiver_aktor_id order by arbeidsforhold_intern_id) as arbeidsforhold,
                      ba.bruker_er_mottaker,
                      ba.inntektskategori,
                      ba.aktivitet_status,
                      ba.arbeidsgiver_aktor_id as personlig_arbeidsgiver,
                      cast(ba.arbeidsforhold_intern_id as varchar) arbeidsforhold_intern_id
                from br_resultat_behandling br
                      inner join br_beregningsresultat res on br.bg_beregningsresultat_fp_id=res.id
                      inner join behandling b on b.id=br.behandling_id
                      inner join fagsak f on f.id=b.fagsak_id
                      inner join br_periode bp on bp.beregningsresultat_fp_id=res.id
                      inner join br_andel ba on ba.br_periode_id=bp.id
                      inner join tmp_aktoer_id a on a.aktoer_id=f.bruker_aktoer_id and a.ident_type='FNR'
                where br.aktiv=true
                      AND f.ytelse_type= :ytelseType
                      AND b.behandling_status = 'AVSLU'
                      AND bp.br_periode_fom <= :tom AND bp.br_periode_tom >= :fom
                      AND ba.dagsats>0
                      AND NOT EXISTS (
                        SELECT 1
                        FROM behandling b2
                        WHERE b2.id != b.id
                              AND b2.opprettet_dato > b.opprettet_dato
                              AND b2.fagsak_id = b.fagsak_id
                              AND b2.behandling_status = 'AVSLU'
                      )
                """;

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("ytelseType", ytelseType.getKode())
            .setParameter("fom", periode.getFomDato())
            .setParameter("tom", periode.getTomDato()) // tar alt overlappende
            .setHint("javax.persistence.query.timeout", 1 * 90 * 1000) // 1:30 min
        ;
        String path = "utbetaling-per-bruker.csv";

        try (Stream<Tuple> stream = query.getResultStream()) {
            return CsvOutput.dumpResultSetToCsv(path, stream)
                .map(v -> List.of(v)).orElse(List.of());
        }

    }

}
