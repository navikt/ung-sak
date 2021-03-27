package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpsters;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpOutput;

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
    public List<DumpOutput> dump(Fagsak fagsak) {
        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId()));
        entityManager.flush();

        var sql = "select"
            + "   f.saksnummer"
            + " , d.fagsak_id"
            + " , replace(cast(d.opprettet_tid as varchar), ' ', 'T') opprettet_tid"
            + " , d.opprettet_av"
            + " from diagnostikk_fagsak_logg d "
            + "   inner join fagsak f on f.id=d.fagsak_id "
            + " where f.saksnummer=:saksnummer "
            + " order by d.opprettet_tid desc";

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "diagnostikk-logg.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return DebugDumpsters.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
