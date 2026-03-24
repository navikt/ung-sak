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
public class KontrollertInntektDump implements DebugDumpBehandling {

    private EntityManager entityManager;

    KontrollertInntektDump() {
        // for proxys
    }

    @Inject
    KontrollertInntektDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        String sql = """
                select
                    kip.periode,
                    kip.inntekt,
                    kip.rapportert_inntekt,
                    kip.register_inntekt,
                    kip.kilde,
                    kip.hjemmel,
                    kip.er_manuelt_vurdert,
                    kip.manuelt_vurdert_begrunnelse
                from gr_kontrollert_inntekt gri
                inner join kontrollert_inntekt_periode kip on kip.kontrollert_inntekt_perioder_id = gri.kontrollert_inntekt_perioder_id
                where gri.aktiv = true
                  and gri.behandling_id = :behandlingid
                order by kip.periode
                """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("behandlingid", behandling.getId());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile(basePath + "/kontrollert-inntekt.csv");
            dumpMottaker.write(output.get());
        }
    }
}
