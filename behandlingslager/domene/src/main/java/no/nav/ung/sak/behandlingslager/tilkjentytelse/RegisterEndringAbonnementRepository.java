package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.typer.AktørId;

import java.util.Optional;

@Dependent
public class RegisterEndringAbonnementRepository {

    private EntityManager entityManager;

    public RegisterEndringAbonnementRepository() {
    }

    @Inject
    public RegisterEndringAbonnementRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagre(RegisterEndringAbonnement abonnement) {
        entityManager.persist(abonnement);
        entityManager.flush();
    }

    public Optional<RegisterEndringAbonnement> hentAbonnementForAktør(AktørId aktørId) {
        var query = entityManager.createQuery(
            "SELECT r FROM RegisterEndringAbonnement r WHERE r.aktørId = :aktørId",
            RegisterEndringAbonnement.class
        );
        query.setParameter("aktørId", aktørId);
        return query.getResultStream().findFirst();
    }

    public void slett(String abonnementId) {
        hentAbonnement(abonnementId).ifPresent(entityManager::remove);
        entityManager.flush();
    }
}

