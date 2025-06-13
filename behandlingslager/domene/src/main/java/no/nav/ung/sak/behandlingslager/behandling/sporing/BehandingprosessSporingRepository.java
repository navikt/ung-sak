package no.nav.ung.sak.behandlingslager.behandling.sporing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;

@ApplicationScoped
public class BehandingprosessSporingRepository {

    private EntityManager entityManager;

    public BehandingprosessSporingRepository() {
        // Default constructor for CDI
    }

    @Inject
    public BehandingprosessSporingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagreSporing(BehandlingprosessSporing sporing) {
        entityManager.persist(sporing);
        entityManager.flush();
    }

    public List<BehandlingprosessSporing> hentSporinger(Long behandlingId, String prosessIdentifikator) {
        return entityManager.createQuery("from BehandlingprosessSporing s where s.behandlingId = :behandlingId and s.prosessIdentifikator = :prosessIdentifikator", BehandlingprosessSporing.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("prosessIdentifikator", prosessIdentifikator)
            .getResultList();
    }

}
