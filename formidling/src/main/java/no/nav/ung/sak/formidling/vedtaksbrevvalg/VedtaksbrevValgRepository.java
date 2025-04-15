package no.nav.ung.sak.formidling.vedtaksbrevvalg;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@Dependent
public class VedtaksbrevValgRepository {

    private final EntityManager entityManager;

    @Inject
    public VedtaksbrevValgRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public VedtaksbrevValgEntitet lagre(VedtaksbrevValgEntitet vedtaksbrevValgEntitet) {
        entityManager.persist(vedtaksbrevValgEntitet);
        return vedtaksbrevValgEntitet;
    }

    public VedtaksbrevValgEntitet finnVedtakbrevValg(Long behandlingId) {
        return entityManager.createQuery(
            "SELECT v FROM VedtaksbrevValgEntitet v WHERE v.behandlingId = :behandlingId and aktiv = true", VedtaksbrevValgEntitet.class)
                .setParameter("behandlingId", behandlingId)
                .getSingleResult();
    }
}
