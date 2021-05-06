package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

@Dependent
public class TmpAktoerIdRepository {

    private EntityManager entityManager;

    @Inject
    public TmpAktoerIdRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<AktørId> finnManglendeMapping(int max) {
        String sql = """
                select distinct f.bruker_aktoer_id from fagsak f
                    where not exists (select 1 from tmp_aktoer_id t where t.aktoer_id=f.bruker_aktoer_id)
                 union select f.pleietrengende_aktoer_id from fagsak f
                    where not exists (select 1 from tmp_aktoer_id t where t.aktoer_id=f.pleietrengende_aktoer_id)
                 union select f.relatert_person_aktoer_id from fagsak f
                    where not exists (select 1 from tmp_aktoer_id t where t.aktoer_id=f.relatert_person_aktoer_id)
                """;

        @SuppressWarnings("unchecked")
        var query = (TypedQuery<String>) entityManager.createNativeQuery(sql);
        query.setMaxResults(max);

        return query.getResultStream().filter(v -> v != null).map(v -> new AktørId(v)).collect(Collectors.toList());

    }

    public void lagre(AktørId aktørId, String ident) {
        var tmp = new TmpAktoerId(aktørId, ident);
        entityManager.persist(tmp);
    }

    public void lagre(Map<AktørId, PersonIdent> mapIdenter) {
        var entiteter = mapIdenter.entrySet().stream().map(e -> new TmpAktoerId(e.getKey(), e.getValue().getIdent())).collect(Collectors.toList());
        for (var e : entiteter) {
            entityManager.persist(e);
        }
        entityManager.flush();
    }
}
