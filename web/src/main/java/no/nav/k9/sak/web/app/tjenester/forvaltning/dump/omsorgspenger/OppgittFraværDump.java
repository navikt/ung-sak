package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.omsorgspenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
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
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
                   select f.saksnummer
                    ,f.id as fagsak_id
                    ,gr.behandling_id
                    ,b.behandling_status
                    ,b.behandling_resultat_type
                    ,omp.fom
                    ,omp.tom
                    ,omp.fravaer_per_dag
                    ,omp.aktivitet
                    ,omp.arbeidsgiver_aktor_id
                    ,omp.arbeidsgiver_orgnr
                    ,cast(omp.arbeidsforhold_intern_id as varchar) arbeidsforhold_intern_id
                    ,omp.journalpost_id
                    ,omp.fravaer_arsak
                    ,omp.soknad_arsak
                    ,replace(cast(omp.opprettet_tid as varchar), ' ', 'T') opprettet_tid
                    from
                    gr_omp_aktivitet gr
                    inner join behandling b on b.id=gr.behandling_id and gr.aktiv=true
                    inner join fagsak f on f.id=b.fagsak_id
                    inner join omp_oppgitt_fravaer_periode omp on omp.fravaer_id=gr.fravaer_id
                    where f.saksnummer=:saksnummer
                    order by 1, 2, 3, omp.opprettet_tid
                """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("omsorgspenger_oppgitt_fravær.csv");
            dumpMottaker.write(output.get());
        }
    }
}
