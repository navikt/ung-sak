package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;

@ApplicationScoped
public class UttalelseRepository {

    @Inject
    private EntityManager entityManager;

    public UttalelseRepository() {}

    public UttalelseRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public UttalelseV2 lagre(UttalelseV2 uttalelse) {
        entityManager.persist(uttalelse);
        entityManager.flush();
        return uttalelse;
    }

    public List<UttalelseV2> lagre(List<UttalelseV2> uttalelser) {
        uttalelser.forEach(this::lagre);
        return uttalelser;
    }

    public List<UttalelseV2> hentUttalelser(Long behandlingId){
        final var uttalelser = entityManager.createQuery("select uv from UttalelseV2 uv " +
                                                         "join Uttalelser u on uv in elements(u.uttalelser) " +
                                                         "join UttalelseGrunnlag ug on u = ug.uttalelser " +
                                                         "where ug.behandlingId = :behandlingId", UttalelseV2.class)
            .setParameter("behandlingId", behandlingId)
            .getResultList();
        return uttalelser;
    }

    public UttalelseV2 hentUttalelse(Long id){
        return entityManager.find(UttalelseV2.class, id);
    }
}
