package no.nav.ung.sak.formidling;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.ung.sak.formidling.domene.BehandlingBrevbestillingEntitet;

@Dependent
public class BrevbestillingRepository {
    private final EntityManager entityManager;

    @Inject
    public BrevbestillingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public List<BehandlingBrevbestillingEntitet> hentForBehandling(Long behandlingId) {
        TypedQuery<BehandlingBrevbestillingEntitet> query = entityManager.createQuery(
            "from BehandlingBrevbestillingEntitet b where b.behandlingId = :behandlingId",
            BehandlingBrevbestillingEntitet.class
        );
        query.setParameter("behandlingId", behandlingId);

        return query.getResultList();
    }

    public void lagre(BehandlingBrevbestillingEntitet bestilling) {
        entityManager.persist(bestilling);
        entityManager.flush();
    }

}
