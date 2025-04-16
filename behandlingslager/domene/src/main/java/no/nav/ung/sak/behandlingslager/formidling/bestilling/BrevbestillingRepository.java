package no.nav.ung.sak.behandlingslager.formidling.bestilling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

@Dependent
public class BrevbestillingRepository {
    private final EntityManager entityManager;

    @Inject
    public BrevbestillingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public List<BehandlingBrevbestillingEntitet> hentForBehandling(Long behandlingId) {
        TypedQuery<BehandlingBrevbestillingEntitet> query = entityManager.createQuery(
            "from BehandlingBrevbestillingEntitet b where b.behandlingId = :behandlingId and aktiv = true",
            BehandlingBrevbestillingEntitet.class
        );
        query.setParameter("behandlingId", behandlingId);

        return query.getResultList();
    }

    public void lagreForBehandling(BehandlingBrevbestillingEntitet bestilling) {
        entityManager.persist(bestilling);
        entityManager.flush();
    }

    public void lagre(BrevbestillingEntitet bestilling) {
        entityManager.persist(bestilling);
        entityManager.flush();
    }

    public BrevbestillingEntitet hent(Long id) {
        TypedQuery<BrevbestillingEntitet> query = entityManager.createQuery(
            "from BrevbestillingEntitet b where b.id = :id and aktiv = true",
            BrevbestillingEntitet.class
        );
        query.setParameter("id", id);

        return query.getSingleResult();

    }

}
