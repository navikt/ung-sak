package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;

@ApplicationScoped
@RapportTypeRef(RapportType.G_REGULERING)
public class UttrekkGreguleringForPeriode implements RapportGenerator {

    private EntityManager entityManager;

    UttrekkGreguleringForPeriode() {
        //
    }

    @Inject
    public UttrekkGreguleringForPeriode(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DumpOutput> generer(FagsakYtelseType ytelseType, DatoIntervallEntitet periode) {
        String sql = """
             select sak.saksnummer,
                      beh.id beh_id,
                      beh.behandling_status,
                      trigger.arsak,
                      lower(trigger.periode) as fom,
                      upper(trigger.periode) as tom
               from prosess_triggere grunnlag
                        inner join pt_triggere triggere on triggere.id = grunnlag.triggere_id
                        inner join pt_trigger trigger on triggere.id = trigger.triggere_id
                        inner join behandling beh on grunnlag.behandling_id = beh.id
                        inner join fagsak sak on beh.fagsak_id = sak.id
               where sak.ytelse_type = :ytelseType
                 and grunnlag.aktiv = true
                 and trigger.arsak = :arsak
                 and trigger.periode && daterange(cast(:fom as date), cast(:tom as date), '[]') = true
            """;

        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("arsak", BehandlingÅrsakType.RE_SATS_REGULERING.getKode())
            .setParameter("ytelseType", ytelseType.getKode())
            .setParameter("fom", periode.getFomDato())
            .setParameter("tom", periode.getTomDato()) // tar alt overlappende
            .setHint("javax.persistence.query.timeout", 1 * 90 * 1000) // 1:30 min
        ;
        String path = "g-regulering.csv";

        try (Stream<Tuple> stream = query.getResultStream()) {
            return CsvOutput.dumpResultSetToCsv(path, stream)
                .map(v -> List.of(v)).orElse(List.of());
        }
    }

}
