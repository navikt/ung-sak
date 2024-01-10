package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.vilkår;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

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
    public void dump(DumpMottaker dumpMottaker) {
        String sql = "select"
            + "  f.saksnummer"
            + " ,rv.behandling_id"
            + " ,vv.vilkar_resultat_id"
            + " ,vv.vilkar_type as vilkar_type_kode"
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
        List<Tuple> results = query.getResultList();
        if (results.isEmpty()) {
            return;
        }

        var valueMapper = settOppMappingAvVilkårType(results);
        String output = CsvOutput.dumpAsCsv(true, results, valueMapper);

        dumpMottaker.newFile("vilkår/resultat.csv");
        dumpMottaker.write(output);
    }

    private static LinkedHashMap<String, Function<Tuple, ?>> settOppMappingAvVilkårType(List<Tuple> results) {
        var valueMapper = new LinkedHashMap<String, Function<Tuple, ?>>();
        for (TupleElement<?> kolonne : results.getFirst().getElements()) {
            valueMapper.put(Optional.ofNullable(kolonne.getAlias()).orElse("col-" + kolonne), t -> t.get(kolonne));
            if (kolonne.getAlias().equals("vilkar_type_kode")) {
                valueMapper.put("vilkar_type", t -> VilkårType.fraKode(t.get(kolonne)).name());
            }
        }
        return valueMapper;
    }
}
