package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.opptjening;

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
@FagsakYtelseTypeRef("OMP")
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("FRISINN")
public class OpptjeningsresultatDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    OpptjeningsresultatDump() {
        // for proxys
    }

    @Inject
    OpptjeningsresultatDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var sql = "select "
            + "  f.saksnummer "
            + ", rs.behandling_id "
            + " , replace(cast(rs.opprettet_tid as varchar), ' ', 'T') opprettet_tid"
            + ", opp.opptjent_periode "
            + ", opp.fom as opp_fom "
            + ", opp.tom as opp_tom "
            + ", oa.opptjeningsperiode_id "
            + ", oa.fom as opp_akt_fom "
            + ", oa.tom as opp_akt_tom "
            + ", oa.aktivitet_type "
            + ", cast(oa.aktivitet_referanse as varchar) aktivitet_referanse"
            + ", oa.klassifisering "
            + ", oa.referanse_type "
            + ", replace(cast(oa.opprettet_tid as varchar), ' ', 'T') oa_opprettet_tid"
            + " from rs_opptjening rs "
            + "  inner join behandling b on b.id=rs.behandling_id "
            + "  inner join fagsak f on f.id=b.fagsak_id "
            + "  inner join opptjening opp on opp.opptjening_resultat_id=rs.id "
            + "  inner join OPPTJENING_AKTIVITET oa on oa.opptjeningsperiode_id=opp.id "
            + " where rs.aktiv=true and f.saksnummer=:saksnummer"
            + " order by f.saksnummer, rs.behandling_id, oa.opprettet_tid";

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "opptjeningsresultat.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return CsvOutput.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
