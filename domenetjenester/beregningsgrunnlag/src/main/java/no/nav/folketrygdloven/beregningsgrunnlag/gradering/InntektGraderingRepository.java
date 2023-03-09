package no.nav.folketrygdloven.beregningsgrunnlag.gradering;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@Dependent
public class InntektGraderingRepository {

    private EntityManager entityManager;

    @Inject
    public InntektGraderingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<Fagsak> hentFagsaker(FagsakYtelseType ytelseType, LocalDate fom) {
        Query query;

        String sql = """
            select f.* from Fagsak f
             where f.ytelse_type = :ytelseType
               and upper(f.periode) > :fom
               and not exists(select 1 from Behandling b inner join
               Aksjonspunkt a on a.behandling_id = b.id
               where b.fagsak_id = f.id and a.aksjonspunkt_def = '5067')
              """;

        query = entityManager.createNativeQuery(sql); // NOSONAR

        query.setParameter("ytelseType", Objects.requireNonNull(ytelseType, "ytelseType").getKode());
        query.setParameter("fom", fom);

        List<Fagsak> result = query.getResultList();
        return result;
    }

    public Long startInntektGraderingForPeriode(FagsakYtelseType ytelseType, LocalDate fom) {
        Objects.requireNonNull(fom);

        String sql = """
            insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
            select nextval('seq_prosess_task'), 'gradering.kandidatUtprøving',
                   nextval('seq_prosess_task_gruppe'), null,
                'fagsakId=' || f.id || ''

                from fagsak f
              where f.ytelse_type = :ytelseType
             and upper(f.periode) > :fom
               and not exists(select 1 from Behandling b inner join
               Aksjonspunkt a on a.behandling_id = b.id
               where b.fagsak_id = f.id and a.aksjonspunkt_def = '5067')
            """;

        var query = entityManager.createNativeQuery(sql); // NOSONAR

        query.setParameter("ytelseType", Objects.requireNonNull(ytelseType, "ytelseType").getKode());
        query.setParameter("fom", fom);


        return Integer.toUnsignedLong(query.executeUpdate());
    }
}
