package no.nav.ung.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

@Dependent
public class TmpAktoerIdRepository {

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskRepository;

    @Inject
    public TmpAktoerIdRepository(EntityManager entityManager, ProsessTaskTjeneste prosessTaskRepository) {
        this.entityManager = entityManager;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public List<AktørId> finnManglendeMapping(int max) {
        String sql = """
                select distinct f.bruker_aktoer_id from fagsak f
                    where not exists (select 1 from tmp_aktoer_id t where t.aktoer_id=f.bruker_aktoer_id)
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
        for (var e : mapIdenter.entrySet()) {
            entityManager.persist(new TmpAktoerId(e.getKey(), e.getValue()));
        }
        entityManager.flush();
    }

    public void resetAktørIdCache() {
        entityManager.createNativeQuery("delete from TMP_AKTOER_ID");
    }

    public void startInnhenting() {
        var cacheFnrTask = ProsessTaskData.forProsessTask(TmpAktørIdTask.class);
        prosessTaskRepository.lagre(cacheFnrTask);
    }
}
