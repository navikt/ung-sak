package no.nav.foreldrepenger.domene.uttak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;

/**
 * Provider for å enklere å kunne hente ut ulike repository uten for mange injection points.
 */
@ApplicationScoped
public class UttakRepositoryProvider {

    private UttakRepository uttakRepository;

    UttakRepositoryProvider() {
        // CDI
    }

    @Inject
    public UttakRepositoryProvider(EntityManager entityManager) {
        // behandling aggregater
        this.uttakRepository = new UttakRepository(entityManager);

    }

    public UttakRepository getUttakRepository() {
        return uttakRepository;
    }

}
