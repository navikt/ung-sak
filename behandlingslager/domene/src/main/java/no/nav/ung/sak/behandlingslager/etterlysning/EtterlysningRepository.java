package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class EtterlysningRepository {

    private EntityManager entityManager;


    @Inject
    public EtterlysningRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagre(Etterlysning etterlysning) {
        entityManager.persist(etterlysning);
    }


}
