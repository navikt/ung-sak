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
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
public class KompletthetDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    KompletthetDump() {
        // for proxys
    }

    @Inject
    KompletthetDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
                 select
                   gr.behandling_id
                   , kgp.skjaeringstidspunkt
                   , kgp.vurdering
                   , kgp.opprettet_tid
                  from gr_beregningsgrunnlag gr
                    inner join BG_KOMPLETT_PERIODER kg on gr.bg_komplett_id=kg.id
                    inner join BG_KOMPLETT_PERIODE kgp on kg.id = kgp.bg_komplett_id
                    inner join behandling BH on GR.behandling_id = BH.id
                    inner join FAGSAK F on BH.fagsak_id = F.id
                   where gr.aktiv=true and f.saksnummer=:saksnummer
                   order by gr.opprettet_tid
                """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("kompletthetsgrunnlag.csv");
            dumpMottaker.write(output.get());
        }
    }
}
