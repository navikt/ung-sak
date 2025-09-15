package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UttalelseRepository {

    @Inject
    private EntityManager entityManager;

    public UttalelseRepository() {}

    public UttalelseRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public void lagre(UttalelseGrunnlag uttalelseGrunnlag) {
        entityManager.persist(uttalelseGrunnlag);
        entityManager.flush();
    }


    public Optional<UttalelseGrunnlag> hentUttalelseBassertPåId(Long behandlingId){
        final var query = entityManager.createQuery(
            "select ug from UttalelseGrunnlag ug " +
                "where ug.behandlingId = :behandlingId", UttalelseGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public UttalelseV2 hentUttalelse(Long id){
        return entityManager.find(UttalelseV2.class, id);
    }
}
