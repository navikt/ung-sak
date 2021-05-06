package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.vilkår;

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
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpsters;

@ApplicationScoped
@FagsakYtelseTypeRef
public class VilkårDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    VilkårDump() {
        // for proxys
    }

    @Inject
    VilkårDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var sql = "select"
            + "  f.saksnummer"
            + " ,rv.behandling_id"
            + " ,vv.vilkar_resultat_id"
            + " ,vv.vilkar_type"
            + " , replace(cast(vv.opprettet_tid as varchar), ' ', 'T') opprettet_tid"
            + " ,vp.vilkar_id"
            + " ,vp.fom"
            + " ,vp.tom"
            + " ,vp.manuelt_vurdert"
            + " ,vp.utfall"
            + " ,vp.merknad"
            + " ,vp.overstyrt_utfall"
            + " ,vp.avslag_kode"
            + " ,vp.merknad_parametere"
            + " , replace(cast(vp.opprettet_tid as varchar), ' ', 'T') vp_opprettet_tid"
            + " ,vp.begrunnelse"
            + " ,regexp_replace(regexp_replace(convert_from(lo_get(cast(regel_input  as oid)), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') as regel_input"
            + " ,regexp_replace(regexp_replace(convert_from(lo_get(cast(regel_evaluering as oid)), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') as regel_evaluering"
            + " from behandling b"
            + "  inner join fagsak f on f.id=b.fagsak_id  "
            + "  inner join rs_vilkars_resultat rv on rv.behandling_id=b.id "
            + "  inner join VR_VILKAR_RESULTAT vvr on vvr.id=rv.vilkarene_id "
            + "  inner join vr_vilkar vv on vv.vilkar_resultat_id=vvr.id "
            + "  inner join vr_vilkar_periode vp on vp.vilkar_id=vv.id  "
            + "where saksnummer=:saksnummer "
            + " and rv.aktiv=true "
            + "order by vilkar_type, vp.opprettet_tid";

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "vilkårresultat.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return CsvOutput.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
