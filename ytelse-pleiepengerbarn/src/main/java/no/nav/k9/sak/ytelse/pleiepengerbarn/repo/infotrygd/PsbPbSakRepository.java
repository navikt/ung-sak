package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.infotrygd;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Dependent
public class PsbPbSakRepository {

    private EntityManager entityManager;


    @Inject
    public PsbPbSakRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    
    public void lagre(Long fagsakId) {
        final Query q = entityManager.createNativeQuery("INSERT INTO PSB_PB_SAK (ID) VALUES (:fagsakId) ON CONFLICT DO NOTHING");
        q.setParameter("fagsakId", fagsakId);
        q.executeUpdate();
        entityManager.flush();
    }
    
    public boolean finnes(Long fagsakId) {
        final Query q = entityManager.createNativeQuery("SELECT ID FROM PSB_PB_SAK WHERE ID = :fagsakId");
        q.setParameter("fagsakId", fagsakId);
        return q.getResultList().size() > 0;
    }
}
