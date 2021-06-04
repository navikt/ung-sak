package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.aksjonspunkt;

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
@FagsakYtelseTypeRef
public class AksjonspunktDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    AksjonspunktDump() {
        // for proxys
    }

    @Inject
    AksjonspunktDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var sql = """
                select f.saksnummer
                     ,b.fagsak_id
                     ,a.behandling_id
                     ,a.aksjonspunkt_def
                     ,a.aksjonspunkt_status
                     ,a.periode_fom
                     ,a.periode_tom
                     ,a.begrunnelse
                     ,a.totrinn_behandling
                     ,a.behandling_steg_funnet
                     ,a.frist_tid
                     ,replace(cast(a.opprettet_tid as varchar), ' ', 'T') opprettet_tid
                     ,a.vent_aarsak
                     ,a.vent_aarsak_variant
                  from aksjonspunkt a
                  inner join behandling b on b.id=a.behandling_id
                  inner join fagsak f on f.id=b.fagsak_id
                  where f.saksnummer=:saksnummer
                 """;

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "aksjonspunkt.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return CsvOutput.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
