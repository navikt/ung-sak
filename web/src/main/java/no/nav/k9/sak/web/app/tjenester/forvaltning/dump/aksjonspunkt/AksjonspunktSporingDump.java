package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.aksjonspunkt;

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
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef
public class AksjonspunktSporingDump implements DebugDumpBehandling {

    private EntityManager entityManager;

    AksjonspunktSporingDump() {
        // for proxys
    }

    @Inject
    AksjonspunktSporingDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        String sql = """
                select a.aksjonspunkt_def
                     ,replace(cast(date_trunc('second', a.opprettet_tid) as varchar), ' ', 'T') opprettet_tid
                        ,a.payload
                  from AKSJONSPUNKT_SPORING a
                  where a.behandling_id = :behandlingId
                  order by a.opprettet_tid
                 """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("behandlingId", behandling.getId());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (!results.isEmpty()) {
            results.forEach(r -> {
                String filename = (String) r.get(0) + "-" + ((String) r.get(1)).replace(":", "_");
                dumpMottaker.newFile(basePath + "/aksjonspunkt-sporing/" + filename + ".json");
                dumpMottaker.write((String) r.get(2));

            });
        }
    }
}
