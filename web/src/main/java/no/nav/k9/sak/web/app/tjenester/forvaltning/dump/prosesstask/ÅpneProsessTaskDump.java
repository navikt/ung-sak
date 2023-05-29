package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.prosesstask;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;

@ApplicationScoped
@FagsakYtelseTypeRef
public class ÅpneProsessTaskDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    ÅpneProsessTaskDump() {
        // for proxys
    }

    @Inject
    ÅpneProsessTaskDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var sql = ""
            + "select f.saksnummer"
            + ", ft.behandling_id"
            + ", ft.gruppe_sekvensnr"
            + ", t.task_type "
            + ", t.status"
            + ", t.task_gruppe"
            + ", t.task_sekvens"
            + ", t.partition_key"
            + ", t.feilede_forsoek"
            + ", t.blokkert_av"
            + ", t.task_parametere"
            + ", regexp_replace(regexp_replace(task_payload, '\\\\n', '\\n'), '\\\\', '') task_payload"
            + ", t.siste_kjoering_feil_tekst"
            + ", replace(cast(t.neste_kjoering_etter as varchar), ' ', 'T') neste_kjoering_etter"
            + ", replace(cast(t.opprettet_tid as varchar), ' ', 'T') opprettet_tid"
            + ", replace(cast(t.siste_kjoering_ts as varchar), ' ', 'T') siste_kjoering_ts"
            + " from prosess_task t "
            + " inner join fagsak_prosess_task ft on ft.prosess_task_id=t.id"
            + " inner join fagsak f on f.id=ft.fagsak_id"
            + " where status in ('FEILET', 'KLAR', 'VETO') and f.saksnummer=:saksnummer";

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "åpne_prosess_task.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return CsvOutput.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
