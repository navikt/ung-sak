package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@ApplicationScoped
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
