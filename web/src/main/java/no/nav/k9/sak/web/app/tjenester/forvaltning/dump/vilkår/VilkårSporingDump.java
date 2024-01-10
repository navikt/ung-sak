package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.vilkår;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef
public class VilkårSporingDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    VilkårSporingDump() {
        // for proxys
    }

    @Inject
    VilkårSporingDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = "select"
            + "  f.saksnummer"
            + " ,rv.behandling_id"
            + " ,vv.vilkar_type"
            + " ,vp.fom"
            + " ,vp.tom"
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

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        Stream<Tuple> stream = query.getResultStream();
        stream.forEachOrdered( result -> {
            var behandlingId = result.get(1);
            var vilkarTypeKode = result.get(2);
            VilkårType vilkårType = VilkårType.fraKode(vilkarTypeKode);
            var fom = result.get(3);
            var tom = result.get(4);
            String input = (String) result.get(5);
            String evaluering = (String) result.get(6);
            dumpMottaker.newFile("vilkår/sporing/behandling-" + behandlingId + "/" + vilkårType + "/" + fom + "_" + tom + "-input.json");
            dumpMottaker.write(input != null ? input : "<manglet>");
            dumpMottaker.newFile("vilkår/sporing/behandling-" + behandlingId + "/" + vilkårType + "/" + fom + "_" + tom + "-evaluering.json");
            dumpMottaker.write(evaluering != null ? evaluering : "<manglet>");
        });
    }
}
