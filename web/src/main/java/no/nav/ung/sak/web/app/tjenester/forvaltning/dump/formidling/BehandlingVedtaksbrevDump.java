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

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef
public class BehandlingVedtaksbrevDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    BehandlingVedtaksbrevDump() {
        // for proxy
    }

    @Inject
    public BehandlingVedtaksbrevDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
            select
                bv.id as behandling_vedtaksbrev_id,
                bv.behandling_id,
                bv.resultat_type,
                bv.beskrivelse,
                bv.opprettet_av,
                bv.opprettet_tid,
                bv.endret_av,
                bv.endret_tid,
                best.id as bestilling_id,
                best.brevbestilling_uuid,
                best.dokumentmal_type,
                best.template_type,
                best.mottaker_id_type,
                best.mottaker_id,
                best.journalpost_id,
                best.dokdist_bestilling_id,
                best.aktiv,
                best.versjon as bestilling_versjon,
                best.vedtaksbrev
            from behandling_vedtaksbrev bv
            left join brevbestilling best on bv.brevbestilling_id = best.id
            where bv.fagsak_id= :fagsak_id
            order by bv.opprettet_tid""";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("fagsak_id", dumpMottaker.getFagsak().getId());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();
        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("behandling_vedtaksbrev.csv");
            dumpMottaker.write(output.get());
        }

    }

}
