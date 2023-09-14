package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.prosesstrigger;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;

@ApplicationScoped
@FagsakYtelseTypeRef
public class ProsesstriggerDump implements DebugDumpBehandling {

    private EntityManager entityManager;

    ProsesstriggerDump() {
        // for proxys
    }

    @Inject
    ProsesstriggerDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        var sql = "select"
            + " pt.arsak, pt.periode "
            + " from prosess_triggere pts "
            + " inner join pt_trigger pt on pt.triggere_id = pts.triggere_id"
            + " where pts.aktiv=true "
            + " and pts.behandling_id=:behandlingid"
            + " order by pt.arsak";


        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("behandlingid", behandling.getId());
        String path = "prosesstriggere.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return CsvOutput.dumpResultSetToCsv(path, results)
            .map(List::of).orElse(List.of());
    }
}
