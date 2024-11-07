package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg;

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

/** Logger tilgang til fagsak for diagnostikk dumps til en egen tabell, inkluderer logg som del av output. */
@ApplicationScoped
@FagsakYtelseTypeRef
public class DiagnostikkLoggDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    DiagnostikkLoggDump() {
        // for proxy
    }

    @Inject
    DiagnostikkLoggDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = "select"
            + "   f.saksnummer"
            + " , d.fagsak_id"
            + " , replace(cast(d.opprettet_tid as varchar), ' ', 'T') opprettet_tid"
            + " , d.opprettet_av"
            + " from diagnostikk_fagsak_logg d "
            + "   inner join fagsak f on f.id=d.fagsak_id "
            + " where f.saksnummer=:saksnummer "
            + " order by d.opprettet_tid desc";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("diagnostikk-logg.csv");
            dumpMottaker.write(output.get());
        }
    }
}
