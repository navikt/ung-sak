package no.nav.k9.sak.behandlingslager.behandling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@Dependent
public class BehandlingOverlappInfotrygdRepository {

    private EntityManager entityManager;

    public BehandlingOverlappInfotrygdRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingOverlappInfotrygdRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long lagre(BehandlingOverlappInfotrygd behandlingOverlappInfotrygd) {
        entityManager.persist(behandlingOverlappInfotrygd);
        entityManager.flush();
        return behandlingOverlappInfotrygd.getId();
    }
}
