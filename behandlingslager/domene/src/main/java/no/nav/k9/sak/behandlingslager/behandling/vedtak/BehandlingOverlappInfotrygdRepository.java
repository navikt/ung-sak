package no.nav.k9.sak.behandlingslager.behandling.vedtak;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

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
