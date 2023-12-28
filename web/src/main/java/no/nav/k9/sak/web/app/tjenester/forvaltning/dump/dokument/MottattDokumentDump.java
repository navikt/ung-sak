package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.dokument;

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
                      f.saksnummer
                    , m.fagsak_id
                    , m.behandling_id
                    , m.id
                    , m.journalpost_id
                    , m.type
                    , replace(cast(m.opprettet_tid as varchar), ' ', 'T') opprettet_tid
                    , m.mottatt_dato
                    , cast(m.forsendelse_id as varchar)
                    , m.dokument_kategori
                    , m.journal_enhet
                    , m.mottatt_tidspunkt
                    , m.kanalreferanse
                    , m.arbeidsgiver
                    , m.status
                    , m.feilmelding
                    , m.kildesystem
                    , replace(cast(m.innsendingstidspunkt as varchar), ' ', 'T') innsendingstidspunkt
                    , regexp_replace(regexp_replace(convert_from(lo_get(payload), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') payload
                   from mottatt_dokument m
                   inner join fagsak f on f.id=m.fagsak_id
                   where f.saksnummer=:saksnummer
                   order by m.mottatt_tidspunkt

                """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("mottatt_dokument.csv");
            dumpMottaker.write(output.get());
        }
    }
}
