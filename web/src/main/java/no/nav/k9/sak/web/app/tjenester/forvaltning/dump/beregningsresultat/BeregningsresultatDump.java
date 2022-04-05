package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.beregningsresultat;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(FRISINN)
public class BeregningsresultatDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    BeregningsresultatDump() {
        // for proxys
    }

    @Inject
    BeregningsresultatDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var sql = """
                   select
                    f.saksnummer
                      ,br.behandling_id
                      ,replace(cast(b.opprettet_tid as varchar), ' ', 'T') opprettet_tid
                      ,bp.br_periode_fom
                      ,bp.br_periode_tom
                      ,ba.dagsats
                      ,ba.inntektskategori
                      ,ba.stillingsprosent
                      ,ba.utbetalingsgrad
                      ,ba.arbeidsforhold_type
                      ,ba.bruker_er_mottaker
                      ,ba.arbeidsgiver_aktor_id
                      ,cast(ba.arbeidsforhold_intern_id as varchar) arbeidsforhold_intern_id
                      ,cast(ba.periode as varchar) periode
                      ,ba.feriepenger_beloep
                      ,regexp_replace(regexp_replace(convert_from(lo_get(regel_input), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') regel_input
                      ,regexp_replace(regexp_replace(convert_from(lo_get(regel_sporing), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') regel_sporing
                      ,regexp_replace(regexp_replace(convert_from(lo_get(feriepenger_regel_input), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') feriepenger_regel_input
                      ,regexp_replace(regexp_replace(convert_from(lo_get(feriepenger_regel_sporing), 'UTF8'), '\\\\n', '\\n'), '\\\\', '') feriepenger_regel_sporing
                     from br_resultat_behandling br
                      inner join br_beregningsresultat res on br.bg_beregningsresultat_fp_id=res.id
                      inner join behandling b on b.id=br.behandling_id
                      inner join fagsak f on f.id=b.fagsak_id
                      inner join br_periode bp on bp.beregningsresultat_fp_id=res.id
                      inner join br_andel ba on ba.br_periode_id=bp.id
                     where br.aktiv=true and f.saksnummer=:saksnummer
                     order by br.behandling_id, bp.br_periode_fom
                """;

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", fagsak.getSaksnummer().getVerdi());
        String path = "beregningsresultat.csv";

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        return CsvOutput.dumpResultSetToCsv(path, results)
            .map(v -> List.of(v)).orElse(List.of());
    }

}
