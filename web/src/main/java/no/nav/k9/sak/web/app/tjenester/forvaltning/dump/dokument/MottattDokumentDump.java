package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.dokument;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef
public class MottattDokumentDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    MottattDokumentDump() {
        // for proxy
    }

    @Inject
    public MottattDokumentDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
            select
               m.journalpost_id
               , regexp_replace(regexp_replace(convert_from(lo_get(payload), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') payload
              from mottatt_dokument m
              inner join fagsak f on f.id=m.fagsak_id
              where f.saksnummer=:saksnummer
              order by m.mottatt_tidspunkt                """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        Stream<Tuple> results = query.getResultStream();
        results.forEachOrdered(result -> {
            Object journalpostId = result.get(0);
            String dokumentinnhold = (String) result.get(1);
            String postfix = avledFormatPostfix(dokumentinnhold);
            String filnavn = "mottatt_dokument/journalpostId-" + journalpostId + postfix;
            dumpMottaker.newFile(filnavn );
            dumpMottaker.write(dokumentinnhold);
        });

    }

    private String avledFormatPostfix(String dokumentinnhold) {
        if (dokumentinnhold.startsWith("<")) {
            return ".xml";
        }
        if (dokumentinnhold.startsWith("{")) {
            return ".json";
        }
        return "";
    }
}
