package no.nav.folketrygdloven.beregningsgrunnlag.regulering;

import java.math.BigInteger;
import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class GReguleringRepository {

    private EntityManager entityManager;

    @Inject
    public GReguleringRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long dryRun(FagsakYtelseType ytelseType, DatoIntervallEntitet periode) {
        Query query;

        String sql = """
            select count(f.saksnummer) from Fagsak f
             where f.ytelse_type = :ytelseType
               and f.periode && daterange(cast(:fom as date), cast(:tom as date), '[]') = true
              """;

        query = entityManager.createNativeQuery(sql); // NOSONAR

        query.setParameter("ytelseType", Objects.requireNonNull(ytelseType, "ytelseType").getKode());
        query.setParameter("fom", periode.getFomDato() == null ? Tid.TIDENES_BEGYNNELSE : periode.getFomDato());
        query.setParameter("tom", periode.getTomDato() == null ? Tid.TIDENES_ENDE : periode.getFomDato());

        return ((BigInteger) query.getSingleResult()).longValueExact();
    }

    public Long startGReguleringForPeriode(FagsakYtelseType ytelseType, DatoIntervallEntitet periode, String fomValue, String tomValue) {
        Objects.requireNonNull(periode);

        String sql = """
            insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
            select nextval('seq_prosess_task'), 'gregulering.kandidatUtprøving',
                   nextval('seq_prosess_task_gruppe'), null,
                        'fom=""" + fomValue + """

            tom=""" + tomValue + """

            fagsakId=' || f.id || ''

                from fagsak f
              where f.ytelse_type = :ytelseType
             and f.periode && daterange(cast(:fom as date), cast(:tom as date), '[]') = true
            """;

        var query = entityManager.createNativeQuery(sql); // NOSONAR

        query.setParameter("ytelseType", Objects.requireNonNull(ytelseType, "ytelseType").getKode());
        query.setParameter("fom", periode.getFomDato());
        query.setParameter("tom", periode.getTomDato());


        return Integer.toUnsignedLong(query.executeUpdate());
    }
}
