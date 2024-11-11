package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.aksjonspunkt;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

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
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
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
                  order by a.opprettet_tid
                 """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("aksjonspunkt.csv");
            dumpMottaker.write(output.get());
        }
    }
}
