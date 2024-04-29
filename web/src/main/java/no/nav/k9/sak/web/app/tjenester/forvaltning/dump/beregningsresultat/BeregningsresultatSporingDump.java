package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.beregningsresultat;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
public class BeregningsresultatSporingDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    BeregningsresultatSporingDump() {
        // for proxys
    }

    @Inject
    BeregningsresultatSporingDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
               select
                f.saksnummer
                  ,br.behandling_id
                  ,replace(cast(b.opprettet_tid as varchar), ' ', 'T') opprettet_tid
                  ,regexp_replace(regexp_replace(convert_from(lo_get(regel_input), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') regel_input
                  ,regexp_replace(regexp_replace(convert_from(lo_get(regel_sporing), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') regel_sporing
                  ,regexp_replace(regexp_replace(convert_from(lo_get(feriepenger_regel_input), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') feriepenger_regel_input
                  ,regexp_replace(regexp_replace(convert_from(lo_get(feriepenger_regel_sporing), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') feriepenger_regel_sporing
                 from br_resultat_behandling br
                  inner join br_beregningsresultat res on br.bg_beregningsresultat_fp_id=res.id
                  inner join behandling b on b.id=br.behandling_id
                  inner join fagsak f on f.id=b.fagsak_id
                 where br.aktiv=true and f.saksnummer=:saksnummer
                 order by br.behandling_id
            """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        Stream<Tuple> results = query.getResultStream();
        results.forEachOrdered(result -> {
            Object behandlingId = result.get(1);
            String filnavnPrefix = "beregningsresultat/sporing/behandling-" + behandlingId + "/";
            dumpMottaker.newFile(filnavnPrefix + "metadata");
            dumpMottaker.write("saksnummer: " + result.get(0) + "\\n");
            dumpMottaker.write("behanlingId: " + behandlingId + "\\n");
            dumpMottaker.write("opprettetTid: " + result.get(2) + "\\n");
            dumpMottaker.newFile(filnavnPrefix + "input.json");
            dumpMottaker.write((String) result.get(3));
            dumpMottaker.newFile(filnavnPrefix + "sporing.json");
            dumpMottaker.write((String) result.get(4));
            dumpMottaker.newFile(filnavnPrefix + "input-feriepenger.json");
            dumpMottaker.write((String) result.get(5));
            dumpMottaker.newFile(filnavnPrefix + "sporing-feriepenger.json");
            dumpMottaker.write((String) result.get(6));
        });
    }
}
