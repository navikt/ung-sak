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
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
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
    public void dump(DumpMottaker dumpMottaker) {
        String sql = """
                   select
                    f.saksnummer
                      ,br.behandling_id
                      ,replace(cast(b.opprettet_tid as varchar), ' ', 'T') opprettet_tid
                      ,bp.br_periode_fom
                      ,bp.br_periode_tom
                      ,bp.gradering_inntekt_prosent
                      ,bp.graderingsfaktor_inntekt
                      ,bp.graderingsfaktor_tid
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
                     from br_resultat_behandling br
                      inner join br_beregningsresultat res on br.bg_beregningsresultat_fp_id=res.id
                      inner join behandling b on b.id=br.behandling_id
                      inner join fagsak f on f.id=b.fagsak_id
                      inner join br_periode bp on bp.beregningsresultat_fp_id=res.id
                      left join br_andel ba on ba.br_periode_id=bp.id
                     where br.aktiv=true and f.saksnummer=:saksnummer
                     order by br.behandling_id, bp.br_periode_fom
                """;

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("beregningsresultat.csv");
            dumpMottaker.write(output.get());
        }
    }
}
