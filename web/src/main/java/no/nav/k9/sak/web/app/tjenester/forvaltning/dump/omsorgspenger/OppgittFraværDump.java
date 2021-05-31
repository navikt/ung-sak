package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.omsorgspenger;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OppgittFraværDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    OppgittFraværDump() {
        // for proxys
    }

    @Inject
    OppgittFraværDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var sql = """
                   select f.saksnummer
                    f.id as fagsak_id
                    gr.behandling_id
                    b.behandling_status
                    b.behandling_resultat_type
                    omp.fom
                    omp.tom
                    omp.fravaer_per_dag
                    omp.aktivitet_type
                    omp.arbeidsgiver_aktor_id
                    omp.arbeidsgiver_orgnr
                    cast(omp.arbeidsforhold_intern_id as varchar) arbeidsforhold_intern_id
                    omp.journalpost_id
                    omp.fravaer_arsak
                    replace(cast(omp.opprettet_tid as varchar), ' ', 'T') opprettet_tid
                    from
                    gr_omp_aktivitet gr
                    inner join behandling b on b.id=gr.behandling_id and gr.aktiv=true
                    inner join fagsak f on f.id=b.fagsak_id
                    inner join omp_oppgitt_fravaer_periode omp on omp.fravaer_id=gr.fravaer_id
                    where f.saksnummer=:saksnummer
                    order by 1, 2, 3, omp.opprettet_tid
                """;

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "omsorgspenger_oppgitt_fravær.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return CsvOutput.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
