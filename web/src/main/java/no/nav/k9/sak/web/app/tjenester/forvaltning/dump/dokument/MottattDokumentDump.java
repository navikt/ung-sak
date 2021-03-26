package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.dokument;

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
    public List<DumpOutput> dump(Fagsak fagsak) {
        String sql = "select "
            + "  f.saksnummer "
            + ", m.fagsak_id "
            + ", m.behandling_id "
            + ", m.id "
            + ", m.journalpost_id "
            + ", m.type "
            + ", replace(cast(m.opprettet_tid as varchar), ' ', 'T') opprettet_tid "
            + ", m.mottatt_dato "
            + ", cast(m.forsendelse_id as varchar) "
            + ", m.dokument_kategori "
            + ", m.journal_enhet "
            + ", m.mottatt_tidspunkt "
            + ", m.kanalreferanse "
            + ", m.arbeidsgiver "
            + ", m.status "
            + ", m.feilmelding "
            + ", m.kildesystem "
            + ", replace(cast(m.innsendingstidspunkt as varchar), ' ', 'T') innsendingstidspunkt "
            + ", convert_from(lo_get(payload), 'UTF8') as payload "
            + "from mottatt_dokument m "
            + "inner join fagsak f on f.id=m.fagsak_id "
            + "where f.saksnummer=:saksnummer";

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "mottatt_dokument.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return DebugDumpsters.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
