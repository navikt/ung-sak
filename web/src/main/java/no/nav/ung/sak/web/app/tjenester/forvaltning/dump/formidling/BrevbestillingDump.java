package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.formidling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef
public class BrevbestillingDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    BrevbestillingDump() {
        // for proxy
    }

    @Inject
    public BrevbestillingDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
            select
                best.id as bestilling_id,
                best.brevbestilling_uuid,
                best.dokumentmal_type,
                best.template_type,
                best.mottaker_id_type,
                best.mottaker_id,
                best.journalpost_id,
                best.dokdist_bestilling_id,
                best.opprettet_av,
                best.opprettet_tid,
                best.endret_av,
                best.endret_tid,
                best.aktiv,
                best.versjon as bestilling_versjon,
                best.vedtaksbrev
            from brevbestilling best
            where best.fagsak_id= :fagsak_id
            order by best.opprettet_tid""";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("fagsak_id", dumpMottaker.getFagsak().getId());

        @SuppressWarnings("unchecked")
        var results = query.getResultList();
        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("brevbestillinger.csv");
            dumpMottaker.write(output.get());
        }

    }

}
