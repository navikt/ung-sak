package no.nav.ung.sak.behandlingslager.formidling.bestilling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@Dependent
public class VedtaksbrevResultatRepository {

    private EntityManager entityManager;

    @Inject
    public VedtaksbrevResultatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public VedtaksbrevResultatEntitet lagre(VedtaksbrevResultatEntitet vedtaksbrevResultat) {
        entityManager.persist(vedtaksbrevResultat);
        return vedtaksbrevResultat;
    }

}
