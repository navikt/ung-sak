package no.nav.k9.sak.behandlingslager.behandling.notat;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;

@Dependent
public class NotatRepository {

    private final EntityManager entityManager;

    @Inject
    public NotatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long opprett(NotatEntitet notat) {
        entityManager.persist(notat);
        entityManager.flush();
        return notat.getId();
    }

    public NotatEntitet hent(Long notatId) {
        TypedQuery<NotatEntitet> query = entityManager.createQuery(
            "select n from NotatEntitet n where n.id = :notatId", NotatEntitet.class);
        query.setParameter("notatId", notatId);
        return HibernateVerktøy.hentEksaktResultat(query);

    }
}
