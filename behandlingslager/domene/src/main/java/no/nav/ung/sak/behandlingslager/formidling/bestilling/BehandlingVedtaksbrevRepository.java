package no.nav.ung.sak.behandlingslager.formidling.bestilling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

@Dependent
public class BehandlingVedtaksbrevRepository {

    private final EntityManager entityManager;

    @Inject
    public BehandlingVedtaksbrevRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public BehandlingVedtaksbrev lagre(BehandlingVedtaksbrev vedtaksbrevResultat) {
        entityManager.persist(vedtaksbrevResultat);
        entityManager.flush();
        return vedtaksbrevResultat;
    }

    public List<BehandlingVedtaksbrev> hentForBehandling(Long behandlingId) {
        TypedQuery<BehandlingVedtaksbrev> query = entityManager.createQuery(
            "from BehandlingVedtaksbrev b where b.behandlingId = :behandlingId",
            BehandlingVedtaksbrev.class
        );
        query.setParameter("behandlingId", behandlingId);

        return query.getResultList();
    }
}
