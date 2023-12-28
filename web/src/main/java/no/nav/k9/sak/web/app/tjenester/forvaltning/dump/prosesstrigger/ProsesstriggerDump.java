package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.prosesstrigger;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

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
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        String sql = "select"
            + " pt.arsak, pt.periode "
            + " from prosess_triggere pts "
            + " inner join pt_trigger pt on pt.triggere_id = pts.triggere_id"
            + " where pts.aktiv=true "
            + " and pts.behandling_id=:behandlingid"
            + " order by pt.arsak";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("behandlingid", behandling.getId());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<DumpOutput> output = CsvOutput.dumpResultSetToCsv(basePath + "/prosesstriggere.csv", results);
        if (output.isPresent()) {
            dumpMottaker.newFile(output.get().getPath());
            dumpMottaker.write(output.get().getContent());
        }
    }
}
