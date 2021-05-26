package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.beregningsgrunnlag;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("FRISINN")
public class BeregningsgrunnlagDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    BeregningsgrunnlagDump() {
        // for proxys
    }

    @Inject
    BeregningsgrunnlagDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var sql = "select"
            + " gr.behandling_id, bgp.skjaeringstidspunkt, cast(bgp.ekstern_referanse as varchar) ekstern_referanse"
            + " from gr_beregningsgrunnlag gr "
            + "   inner join bg_perioder bg on gr.bg_grunnlag_id=bg.id"
            + "   inner join bg_periode bgp on bg.id = bgp.bg_grunnlag_id"
            + "   inner join behandling BH on GR.behandling_id = BH.id"
            + "   inner join FAGSAK F on BH.fagsak_id = F.id"
            + " where gr.aktiv=true and f.saksnummer=:saksnummer "
            + " order by gr.opprettet_tid";

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "beregningsgrunnlag.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return CsvOutput.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
