package no.nav.ung.sak.behandlingslager.formidling.bestilling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

@Dependent
public class VedtaksbrevResultatRepository {

    private final EntityManager entityManager;

    @Inject
    public VedtaksbrevResultatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public VedtaksbrevResultatEntitet lagre(VedtaksbrevResultatEntitet vedtaksbrevResultat) {
        entityManager.persist(vedtaksbrevResultat);
        entityManager.flush();
        return vedtaksbrevResultat;
    }

    public List<VedtaksbrevResultatEntitet> hentForBehandling(Long behandlingId) {
        TypedQuery<VedtaksbrevResultatEntitet> query = entityManager.createQuery(
            "from VedtaksbrevResultatEntitet b where b.behandlingId = :behandlingId",
            VedtaksbrevResultatEntitet.class
        );
        query.setParameter("behandlingId", behandlingId);

        return query.getResultList();
    }
}
