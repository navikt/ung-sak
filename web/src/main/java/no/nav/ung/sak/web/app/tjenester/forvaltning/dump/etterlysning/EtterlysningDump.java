package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef
public class EtterlysningDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    EtterlysningDump() {
        // for proxys
    }

    @Inject
    EtterlysningDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
                select f.saksnummer
                     ,b.fagsak_id
                     ,e.behandling_id
                     ,e.type
                     ,e.status
                     ,e.fom
                     ,e.tom
                     ,e.grunnlag_ref
                     ,e.ekstern_ref
                     ,e.frist
                  from etterlysning e
                  inner join behandling b on b.id=e.behandling_id
                  inner join fagsak f on f.id=b.fagsak_id
                  where f.saksnummer=:saksnummer
                  order by e.opprettet_tid
                 """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("etterlysning.csv");
            dumpMottaker.write(output.get());
        }
    }
}
