package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.sak.typer.AktørId;

import java.util.Optional;

@Dependent
public class RegisterInntektAbonnementRepository {

    private EntityManager entityManager;

    public RegisterInntektAbonnementRepository() {
    }

    @Inject
    public RegisterInntektAbonnementRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagre(RegisterInntektAbonnement abonnement) {
        entityManager.persist(abonnement);
        entityManager.flush();
    }

    public Optional<RegisterInntektAbonnement> hentAbonnement(String abonnementId) {
        return Optional.ofNullable(entityManager.find(RegisterInntektAbonnement.class, abonnementId));
    }

    public Optional<RegisterInntektAbonnement> hentAbonnementForAktør(AktørId aktørId) {
        var query = entityManager.createQuery(
            "SELECT r FROM RegisterInntektAbonnement r WHERE r.aktørId = :aktørId",
            RegisterInntektAbonnement.class
        );
        query.setParameter("aktørId", aktørId);
        return query.getResultStream().findFirst();
    }

    public void slettAbonnement(RegisterInntektAbonnement abonnement) {
        entityManager.remove(abonnement);
    }

}

