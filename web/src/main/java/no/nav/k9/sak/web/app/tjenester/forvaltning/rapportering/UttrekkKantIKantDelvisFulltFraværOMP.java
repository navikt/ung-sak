package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;

@ApplicationScoped
@RapportTypeRef(RapportType.DELVIS_FULLT_KANTIKANT)
public class UttrekkKantIKantDelvisFulltFraværOMP implements RapportGenerator {

    private EntityManager entityManager;

    UttrekkKantIKantDelvisFulltFraværOMP() {
        //
    }

    @Inject
    public UttrekkKantIKantDelvisFulltFraværOMP(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DumpOutput> generer(FagsakYtelseType ytelseType, DatoIntervallEntitet periode) {
        String sql = """
             with delvis_fravær as
             (select * from omp_oppgitt_fravaer_periode where fravaer_per_dag != '0')
             , fullt_fravær as
             (select * from omp_oppgitt_fravaer_periode where coalesce(fravaer_per_dag, '0') = '0')
             select f.saksnummer, beh.id beh_id, beh.opprettet_tid
             from delvis_fravær
             inner join fullt_fravær on delvis_fravær.fravaer_id = fullt_fravær.fravaer_id
             inner join omp_oppgitt_fravaer oof on oof.id = delvis_fravær.fravaer_id
             inner join gr_omp_aktivitet gr on gr.fravaer_id = oof.id and gr.aktiv = true
             inner join behandling beh on beh.id = gr.behandling_id
             inner join fagsak f on beh.fagsak_id = f.id
             where delvis_fravær.tom + interval '1' day = fullt_fravær.fom
             and gr.aktiv = true
             and beh.opprettet_tid between cast(:fom as timestamp) and cast(:tom as timestamp)
            """;

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("fom", periode.getFomDato())
            .setParameter("tom", periode.getTomDato())
            .setHint("jakarta.persistence.query.timeout", 1 * 90 * 1000) // 1:30 min
        ;
        String path = "delvis-helt-kantIKant.csv";

        try (Stream<Tuple> stream = query.getResultStream()) {
            return CsvOutput.dumpResultSetToCsv(path, stream)
                .map(v -> List.of(v)).orElse(List.of());
        }
    }

}
