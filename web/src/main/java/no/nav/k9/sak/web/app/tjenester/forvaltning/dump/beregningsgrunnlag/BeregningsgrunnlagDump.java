package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
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
        var sql = """
                 select
                   gr.behandling_id
                   , bgp.skjaeringstidspunkt
                   , cast(bgp.ekstern_referanse as varchar) ekstern_referanse
                  from gr_beregningsgrunnlag gr
                    inner join bg_perioder bg on gr.bg_grunnlag_id=bg.id
                    inner join bg_periode bgp on bg.id = bgp.bg_grunnlag_id
                    inner join behandling BH on GR.behandling_id = BH.id
                    inner join FAGSAK F on BH.fagsak_id = F.id
                   where gr.aktiv=true and f.saksnummer=:saksnummer
                   order by gr.opprettet_tid
                """;

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

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
                 select
                   gr.behandling_id
                   , bgp.skjaeringstidspunkt
                   , cast(bgp.ekstern_referanse as varchar) ekstern_referanse
                  from gr_beregningsgrunnlag gr
                    inner join bg_perioder bg on gr.bg_grunnlag_id=bg.id
                    inner join bg_periode bgp on bg.id = bgp.bg_grunnlag_id
                    inner join behandling BH on GR.behandling_id = BH.id
                    inner join FAGSAK F on BH.fagsak_id = F.id
                   where gr.aktiv=true and f.saksnummer=:saksnummer
                   order by gr.opprettet_tid
                """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<DumpOutput> output = CsvOutput.dumpResultSetToCsv("beregningsgrunnlag.csv", results);
        if (output.isPresent()) {
            dumpMottaker.newFile(output.get().getPath());
            dumpMottaker.write(output.get().getContent());
        }
    }
}
