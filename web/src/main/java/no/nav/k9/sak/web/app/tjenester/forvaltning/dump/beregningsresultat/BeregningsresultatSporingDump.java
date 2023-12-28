package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.beregningsresultat;

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
    public List<DumpOutput> dump(Fagsak fagsak) {
        var sql = """
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

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "beregningsresultat-input-og-sporing.csv";

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
        List<Tuple> results = query.getResultList();

        Optional<DumpOutput> output = CsvOutput.dumpResultSetToCsv("beregningsresultat-input-og-sporing.csv", results);
        if (output.isPresent()) {
            dumpMottaker.newFile(output.get().getPath());
            dumpMottaker.write(output.get().getContent());
        }
    }
}
