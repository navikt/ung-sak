package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.typer.AktørId;
import java.util.Optional;

@Dependent
public class InntektAbonnementRepository {

    private EntityManager entityManager;

    @Inject
    public InntektAbonnementRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagre(InntektAbonnement abonnement) {
        entityManager.persist(abonnement);
    }

    public Optional<InntektAbonnement> hentAbonnementForAktør(AktørId aktørId) {
        var query = entityManager.createQuery(
            "SELECT r FROM InntektAbonnement r WHERE r.aktørId = :aktørId",
            InntektAbonnement.class
        );
        query.setParameter("aktørId", aktørId);
        return query.getResultStream().findFirst();
    }

    public void slettAbonnement(InntektAbonnement abonnement) {
        abonnement.setAktiv(false);
        entityManager.persist(abonnement);
    }
}

