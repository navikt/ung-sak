package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.tilkjentytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef
public class TilkjentYtelseDump implements DebugDumpBehandling {

    private EntityManager entityManager;

    TilkjentYtelseDump() {
        // for proxys
    }

    @Inject
    TilkjentYtelseDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        String sql = """
                select
                    typ.periode,
                    typ.uredusert_belop,
                    typ.reduksjon,
                    typ.redusert_belop,
                    typ.dagsats,
                    typ.utbetalingsgrad
                from tilkjent_ytelse ty
                inner join tilkjent_ytelse_periode typ on typ.tilkjent_ytelse_id = ty.id
                where ty.aktiv = true
                  and ty.behandling_id = :behandlingid
                order by typ.periode
                """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("behandlingid", behandling.getId());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile(basePath + "/tilkjent-ytelse.csv");
            dumpMottaker.write(output.get());
        }
    }
}
