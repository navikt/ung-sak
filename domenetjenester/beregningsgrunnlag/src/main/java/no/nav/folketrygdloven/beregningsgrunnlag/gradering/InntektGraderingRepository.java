package no.nav.folketrygdloven.beregningsgrunnlag.gradering;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class InntektGraderingRepository {

    private EntityManager entityManager;

    @Inject
    public InntektGraderingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Map<Long, Saksnummer> hentFagsakIdOgSaksnummer(FagsakYtelseType ytelseType, LocalDate fom) {
        Query query;

        String sql = """
            select f.id, f.saksnummer from Fagsak f
             where f.ytelse_type = :ytelseType
               and upper(f.periode) > :fom
               and not exists(select 1 from Behandling b inner join
               Aksjonspunkt a on a.behandling_id = b.id
               where b.fagsak_id = f.id and a.aksjonspunkt_def = '5067')
              """;

        query = entityManager.createNativeQuery(sql); // NOSONAR

        query.setParameter("ytelseType", Objects.requireNonNull(ytelseType, "ytelseType").getKode());
        query.setParameter("fom", fom);

        List<Object[]> result = query.getResultList();

        return result.stream().collect(Collectors.toMap(
            o -> (Long) o[0],
            o -> new Saksnummer((String) o[1])
        ));
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
